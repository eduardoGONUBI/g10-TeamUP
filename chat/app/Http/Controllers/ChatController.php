<?php

namespace App\Http\Controllers;

use App\Models\EventUser;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
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
        $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
        $queueName = env('RABBITMQ_QUEUE', 'event_joined');

        try {
            // Connect to RabbitMQ
            $connection = new AMQPStreamConnection($rabbitmqHost, $rabbitmqPort, $rabbitmqUser, $rabbitmqPassword);
            $channel = $connection->channel();

            // Declare the queue with durable=true
            $channel->queue_declare($queueName, false, true, false, false);

            // Set prefetch count to control the number of unacknowledged messages per consumer
            $channel->basic_qos(null, 1, null);

            // Define the callback for consuming messages
            $callback = function ($msg) {
                echo 'Message received: ', $msg->body, "\n";

                // Log the received message
               // \Log::info('Message received by RabbitMQ listener:', ['message' => $msg->body]);

                // Decode the message body
                $messageData = json_decode($msg->body, true);

                // Save the message to MySQL using EventUser model
                EventUser::create([
                    'event_id' => $messageData['event_id'],
                    'event_name' => $messageData['event_name'],
                    'user_id' => $messageData['user_id'],
                    'user_name' => $messageData['user_name'],
                    'message' => $messageData['message'],
                ]);

                // Manually acknowledge the message after processing
                $msg->ack();
            };

            // Start consuming messages from the queue
            $channel->basic_consume($queueName, '', false, false, false, false, $callback);

            // Keep listening for incoming messages
            while ($channel->is_consuming()) {
                $channel->wait();
            }

            // Close channel and connection when done
            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
            //\Log::error('Error in listenRabbitMQ:', ['error' => $e->getMessage()]);
        }
    }

/**
 * Send a message in an event chat.
 */
/**
 * Send a message in an event chat.
 */
public function sendMessage(Request $request, $id)
{
    try {
        // Validate the token
        $token = $this->validateToken($request);

        // Parse the token to get the user info
        $payload = JWTAuth::setToken($token)->getPayload();
        $userId = $payload->get('sub');
        $userName = $payload->get('name');

        // Validate the message input
        $validatedData = $request->validate([
            'message' => 'required|string',
        ]);

        // Check if the user is participating in the event
        $isParticipating = EventUser::where('event_id', $id)
            ->where('user_id', $userId)
            ->exists();

        if (!$isParticipating) {
           /* \Log::warning('Unauthorized attempt to send a message to an event:', [
                'user_id' => $userId,
                'event_id' => $id,
            ]);*/
            return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
        }

        // Store the message in the event_user table
        $messageData = [
            'event_id'   => $id,
            'event_name' => "Evento $id",
            'user_id'    => $userId,
            'user_name'  => $userName,
            'message'    => $validatedData['message'],
        ];

        EventUser::create($messageData);

        // Fetch all unique participants of the event
        $eventParticipants = EventUser::where('event_id', $id)
            ->distinct()
            ->get(['user_id', 'user_name']);

        // Prepare the notification message with distinct participants
        $notificationMessage = [
            'type'        => 'new_message',
            'event_id'    => $id,
            'event_name'  => "Evento $id",
            'user_id'     => $userId,
            'user_name'   => $userName,
            'message'     => $validatedData['message'],
            'timestamp'   => now()->toISOString(),
            'participants'=> $eventParticipants->toArray(),
        ];

        // Publish the message to the RabbitMQ queue named 'notification'
        $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

        return response()->json(['status' => 'Message sent successfully'], 201);
    } catch (\Illuminate\Validation\ValidationException $e) {
        return response()->json([
            'message' => 'Validation Error',
            'errors' => $e->errors(),
        ], 422);
    } catch (\Exception $e) {
       // \Log::error('Error in sendMessage:', ['error' => $e->getMessage(), 'trace' => $e->getTraceAsString()]);
        return response()->json(['error' => $e->getMessage()], 401);
    }
}


/**
 * Publish a message to RabbitMQ.
 */
private function publishToRabbitMQ($queueName, $messageBody)
{
    $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
    $rabbitmqPort = env('RABBITMQ_PORT', 5672);
    $rabbitmqUser = env('RABBITMQ_USER', 'guest');
    $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');

    try {
        // Connect to RabbitMQ
        $connection = new AMQPStreamConnection($rabbitmqHost, $rabbitmqPort, $rabbitmqUser, $rabbitmqPassword);
        $channel = $connection->channel();

        // Declare the queue to ensure it exists
        $channel->queue_declare($queueName, false, true, false, false);

        // Create the message
        $msg = new AMQPMessage($messageBody);

        // Publish the message to the specified queue
        $channel->basic_publish($msg, '', $queueName);

        // Close the channel and connection
        $channel->close();
        $connection->close();

       // \Log::info('Message published to RabbitMQ:', ['queue' => $queueName, 'message' => $messageBody]);
    } catch (\Exception $e) {
      //  \Log::error('Error publishing message to RabbitMQ:', ['error' => $e->getMessage(), 'trace' => $e->getTraceAsString()]);
    }
}



    /**
     * Fetch messages for an event.
     */
    public function fetchMessages(Request $request, $id)
    {
        try {
            // Validate the token
            $token = $this->validateToken($request);

            // Parse the token to get the user info
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');

            // Check if the user is participating in the event by querying the event_user table
            $isParticipating = EventUser::where('event_id', $id)
                ->where('user_id', $userId)
                ->exists();

            if (!$isParticipating) {
             /*   \Log::warning('Unauthorized attempt to fetch messages for an event:', [
                    'user_id' => $userId,
                    'event_id' => $id,
                ]);*/
                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            // Fetch all messages from the event_user table for the event
            $messages = EventUser::where('event_id', $id)->get();

            return response()->json(['messages' => $messages], 200);
        } catch (\Exception $e) {
          //  \Log::error('Error in fetchMessages:', ['error' => $e->getMessage()]);
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

        if (!$token) {
            throw new \Exception('Token is required.');
        }

        // Strip "Bearer " prefix if present
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
