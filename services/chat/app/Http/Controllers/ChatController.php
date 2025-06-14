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

    //───────────────────────────────────────────── RABBIT ────────────────────────────────────────────────────────────────────────────────────

    //Escuta uma fila do rabbit  e regista na BD 
    public function listenRabbitMQ()
    {
        $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
        $queueName = env('RABBITMQ_QUEUE', 'lolchat_event_join-leave');

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

                $messageData = json_decode($msg->body, true);

                EventUser::create([             //guarda na base de dados          
                    'event_id' => $messageData['event_id'],
                    'event_name' => $messageData['event_name'],
                    'user_id' => $messageData['user_id'],
                    'user_name' => $messageData['user_name'],
                    'message' => $messageData['message'],
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

        }
    }

    // pubica no rabbit
    private function publishToRabbitMQ(string $messageBody)
    {
        $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');

        // Fanout exchanges + target queue
        $fanoutExchanges = [
            'notification_fanout_1',
            'notification_fanout_2',
        ];
        $directQueue = 'realfrontchat';

        try {
            $connection = new AMQPStreamConnection(
                $rabbitmqHost,
                $rabbitmqPort,
                $rabbitmqUser,
                $rabbitmqPassword
            );
            $channel = $connection->channel();

            // Publish to fanout exchanges
            foreach ($fanoutExchanges as $exchange) {
                $channel->exchange_declare(
                    $exchange,
                    'fanout',
                    false,
                    true,
                    false
                );

                $msg = new AMQPMessage($messageBody, [
                    'content_type' => 'application/json',
                    'delivery_mode' => AMQPMessage::DELIVERY_MODE_PERSISTENT,
                ]);

                $channel->basic_publish($msg, $exchange);

            }

            // Ensure direct queue exists
            $channel->queue_declare($directQueue, false, true, false, false);

            // Publish directly to the queue using default exchange ("")
            $directMsg = new AMQPMessage($messageBody, [
                'content_type' => 'application/json',
                'delivery_mode' => AMQPMessage::DELIVERY_MODE_PERSISTENT,
            ]);
            $channel->basic_publish($directMsg, '', $directQueue);


            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
        }
    }

    //───────────────────────────────────────────── SEND MESSAGE────────────────────────────────────────────────────────────────────────────────────
    public function sendMessage(Request $request, $id)
    {
        try { // valida token e extrai id e name
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            // valida a mensagem
            $validatedData = $request->validate([
                'message' => 'required|string',
            ]);

            // verifica se o user é participante
            $isParticipating = EventUser::where('event_id', $id)
                ->where('user_id', $userId)
                ->exists();

            if (!$isParticipating) {

                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            // guarda na BD
            $messageData = [
                'event_id' => $id,
                'event_name' => "Evento $id",
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => $validatedData['message'],
            ];
            EventUser::create($messageData);

            // obtem lista de participantes
            $eventParticipants = EventUser::where('event_id', $id)
                ->distinct()
                ->get(['user_id', 'user_name']);

            // faz o payload
            $notificationMessage = [
                'type' => 'new_message',
                'event_id' => $id,
                'event_name' => "Evento $id",
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => $validatedData['message'],
                'timestamp' => now()->toISOString(),
                'participants' => $eventParticipants->toArray(),
            ];

            //  publica no rabbit
            $this->publishToRabbitMQ(json_encode($notificationMessage));

            return response()->json(['status' => 'Message sent successfully'], 201); // sucesso
        } catch (\Illuminate\Validation\ValidationException $e) {
            return response()->json([
                'message' => 'Validation Error',
                'errors' => $e->errors(),
            ], 422);
        } catch (\Exception $e) {

            return response()->json(['error' => $e->getMessage()], 401);
        }
    }




    //───────────────────────────────────────────── FETCH MESSAGE────────────────────────────────────────────────────────────────────────────────────
    public function fetchMessages(Request $request, $id)
    {
        try {
            // valida token e extrai id
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');

            // verifica se o utilizador participa no evento
            $isParticipating = EventUser::where('event_id', $id)
                ->where('user_id', $userId)
                ->exists();

            if (!$isParticipating) {

                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            // fetch a todas as mensagens gravadas na BD
            $messages = EventUser::where('event_id', $id)->get();

            return response()->json(['messages' => $messages], 200);  // sucesso retorna array de mensagens
        } catch (\Exception $e) {

            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    //───────────────────────────────────────────── token ────────────────────────────────────────────────────────────────────────────────────
    //verifica se token esta blacklisted
    private function isTokenBlacklisted($token)
    {
        $cacheKey = "blacklisted:{$token}";

        return Cache::has($cacheKey);
    }

    //extrai token do pedido
    private function getTokenFromRequest(Request $request)
    {
        $token = $request->header('Authorization');

        if (!$token) {
            throw new \Exception('Token is required.');
        }

        if (str_starts_with($token, 'Bearer ')) {
            $token = substr($token, 7);
        }

        return $token;
    }


    //  valida token e verifica se esta blacklisted

    private function validateToken(Request $request)
    {
        $token = $this->getTokenFromRequest($request);

        if ($this->isTokenBlacklisted($token)) {
            throw new \Exception('Unauthorized: Token is blacklisted.');
        }

        return $token;
    }
}
