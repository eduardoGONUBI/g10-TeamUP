<?php

namespace App\Http\Controllers;

use App\Models\EventUser;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Tymon\JWTAuth\Facades\JWTAuth;
use PhpAmqpLib\Connection\AMQPStreamConnection;
use PhpAmqpLib\Message\AMQPMessage;

class ChatController extends Controller
{
    /**
     * Listen to RabbitMQ queue and display messages being published.
     */
    public function listenRabbitMQ()
    {
        $rabbitmqHost     = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort     = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser     = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
        $queueName        = env('RABBITMQ_QUEUE', 'event_joined');

        try {
            $connection = new AMQPStreamConnection(
                $rabbitmqHost,
                $rabbitmqPort,
                $rabbitmqUser,
                $rabbitmqPassword
            );
            $channel = $connection->channel();

            $channel->queue_declare($queueName, false, true, false, false);
            $channel->basic_qos(null, 1, null);

            $callback = function ($msg) {
                echo 'Message received: ', $msg->body, "\n";
                \Log::info('Message received by RabbitMQ listener:', ['message' => $msg->body]);
                $messageData = json_decode($msg->body, true);

                EventUser::create([
                    'event_id'   => $messageData['event_id'],
                    'event_name' => $messageData['event_name'],
                    'user_id'    => $messageData['user_id'],
                    'user_name'  => $messageData['user_name'],
                    'message'    => $messageData['message'],
                ]);

                $msg->ack();
            };

            $channel->basic_consume($queueName, '', false, false, false, false, $callback);

            while ($channel->is_consuming()) {
                $channel->wait();
            }

            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
            \Log::error('Error in listenRabbitMQ:', ['error' => $e->getMessage()]);
        }
    }

    /**
     * Send a message in an event chat.
     */
    public function sendMessage(Request $request, $id)
    {
        try {
            $token   = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId   = $payload->get('sub');
            $userName = $payload->get('name');

            $validatedData = $request->validate([
                'message' => 'required|string',
            ]);

            $isParticipating = EventUser::where('event_id', $id)
                ->where('user_id', $userId)
                ->exists();

            if (! $isParticipating) {
                \Log::warning('Unauthorized attempt to send a message to an event:', [
                    'user_id'  => $userId,
                    'event_id' => $id,
                ]);
                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            $messageData = [
                'event_id'   => $id,
                'event_name' => "Evento $id",
                'user_id'    => $userId,
                'user_name'  => $userName,
                'message'    => $validatedData['message'],
            ];
            EventUser::create($messageData);

            $eventParticipants = EventUser::where('event_id', $id)
                ->distinct()
                ->get(['user_id', 'user_name']);

            $notificationMessage = [
                'type'         => 'new_message',
                'event_id'     => $id,
                'event_name'   => "Evento $id",
                'user_id'      => $userId,
                'user_name'    => $userName,
                'message'      => $validatedData['message'],
                'timestamp'    => now()->toISOString(),
                'participants' => $eventParticipants->toArray(),
            ];

            // Publish the message to two hardcoded fanout exchanges
            $this->publishToRabbitMQ(json_encode($notificationMessage));

            return response()->json(['status' => 'Message sent successfully'], 201);
        } catch (\Illuminate\Validation\ValidationException $e) {
            return response()->json([
                'message' => 'Validation Error',
                'errors'  => $e->errors(),
            ], 422);
        } catch (\Exception $e) {
            \Log::error('Error in sendMessage:', ['error' => $e->getMessage(), 'trace' => $e->getTraceAsString()]);
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    /**
     * Publish a message to two RabbitMQ fanout exchanges.
     */
    private function publishToRabbitMQ(string $messageBody)
    {
        $rabbitmqHost     = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort     = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser     = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');

        // Two hardcoded fanout exchanges
        $exchanges = [
            'notification_fanout_1',
            'notification_fanout_2',
        ];

        try {
            $connection = new AMQPStreamConnection(
                $rabbitmqHost,
                $rabbitmqPort,
                $rabbitmqUser,
                $rabbitmqPassword
            );
            $channel = $connection->channel();

            foreach ($exchanges as $exchange) {
                $channel->exchange_declare(
                    $exchange,
                    'fanout',
                    false,
                    true,
                    false
                );

                $msg = new AMQPMessage($messageBody, [
                    'content_type'  => 'application/json',
                    'delivery_mode' => AMQPMessage::DELIVERY_MODE_PERSISTENT,
                ]);

                $channel->basic_publish($msg, $exchange);
                \Log::info("Published message to exchange '{$exchange}'", ['body' => $messageBody]);
            }

            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
            \Log::error('Error publishing to RabbitMQ exchanges:', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);
        }
    }

    /**
     * Fetch messages for an event.
     */
    public function fetchMessages(Request $request, $id)
    {
        try {
            $token   = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId  = $payload->get('sub');

            $isParticipating = EventUser::where('event_id', $id)
                ->where('user_id', $userId)
                ->exists();

            if (! $isParticipating) {
                \Log::warning('Unauthorized attempt to fetch messages for an event:', [
                    'user_id' => $userId,
                    'event_id' => $id,
                ]);
                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            $messages = EventUser::where('event_id', $id)->get();

            return response()->json(['messages' => $messages], 200);
        } catch (\Exception $e) {
            \Log::error('Error in fetchMessages:', ['error' => $e->getMessage()]);
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    /**
     * Check if a token is blacklisted using the /get-blacklist endpoint.
     */
    private function isTokenBlacklisted($token)
    {
        $cacheKey = "blacklisted:{$token}";

        return Cache::has($cacheKey);
    }

    /**
     * Process token from Authorization header.
     */
    private function getTokenFromRequest(Request $request)
    {
        $token = $request->header('Authorization');

        if (! $token) {
            throw new \Exception('Token is required.');
        }

        if (str_starts_with($token, 'Bearer ')) {
            $token = substr($token, 7);
        }

        return $token;
    }

    /**
     * Validate token and check if blacklisted.
     */
    private function validateToken(Request $request)
    {
        $token = $this->getTokenFromRequest($request);

        if ($this->isTokenBlacklisted($token)) {
            throw new \Exception('Unauthorized: Token is blacklisted.');
        }

        return $token;
    }
}
