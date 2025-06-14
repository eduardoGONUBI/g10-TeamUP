<?php

namespace App\Http\Controllers;

use App\Models\EventUser;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Http;
use Tymon\JWTAuth\Facades\JWTAuth;
use PhpAmqpLib\Connection\AMQPStreamConnection;
use PhpAmqpLib\Message\AMQPMessage;
use App\Models\EventRating;
use App\Models\UserAverage;
use App\Models\UserReputation;
use App\Models\EventFeedback;
use Illuminate\Support\Facades\DB;

class ChatController extends Controller
{


    //───────────────────────────────────────────── GIVE FEEDBACK ────────────────────────────────────────────────────────────────────────────────────

    public function giveFeedback(Request $request, $event_id)
    {
        try {
            // valida token e extrai id
            $token = $this->validateToken($request);
            $raterId = JWTAuth::setToken($token)->getPayload()->get('sub');



            // valida payload
            $validated = $request->validate([
                'user_id' => 'required|integer',
                'attribute' => 'required|string',
            ]);
            $ratedId = (int) $validated['user_id'];
            $attribute = $validated['attribute'];

            // impede uma autoavaliaçao
            if ($ratedId === (int) $raterId) {
                return response()->json(['error' => 'You cannot rate yourself'], 400);
            }

            // Define attributos
            $pos = ['good_teammate', 'friendly', 'team_player'];
            $neg = ['toxic', 'bad_sport', 'afk'];

            if (!in_array($attribute, array_merge($pos, $neg))) {
                return response()->json(['error' => 'Unknown attribute'], 422);
            }
            // Atributos positivos somam +3 /  negativos subtraem -5
            $delta = in_array($attribute, $pos) ? 3 : -5;

            // verifica se evento foi concluido
            $isConcluded = EventUser::where('event_id', $event_id)
                ->where('message', 'like', '%- concluded')
                ->exists();
            if (!$isConcluded) {
                return response()->json(['error' => 'Event not concluded'], 400);
            }

            // impede duplicaçao do mesmo atributo
            $dup = EventFeedback::where([
                'event_id' => $event_id,
                'rater_id' => $raterId,
                'rated_id' => $ratedId,
                'attribute' => $attribute,
            ])->exists();
            if ($dup) {
                return response()->json(['error' => 'Already given this feedback'], 400);
            }

            // insere o feedback e atualiza a reputaçao
            DB::transaction(function () use ($event_id, $raterId, $ratedId, $attribute, $delta) {

                EventFeedback::create([
                    'event_id' => $event_id,
                    'rater_id' => $raterId,
                    'rated_id' => $ratedId,
                    'attribute' => $attribute,
                    'delta' => $delta,
                ]);

                // mapeia atributos
                $column = match ($attribute) {
                    'good_teammate' => 'good_teammate_count',
                    'friendly' => 'friendly_count',
                    'team_player' => 'team_player_count',
                    'toxic' => 'toxic_count',
                    'bad_sport' => 'bad_sport_count',
                    'afk' => 'afk_count',
                };

                // atualiza ou cria a reputaçao do utilizador avaliado
                DB::statement(
                    "
                INSERT INTO user_reputations
                    (user_id, score, {$column}, created_at, updated_at)
                VALUES
                    (?, LEAST(100, GREATEST(0, 70 + ?)), 1, NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    score      = LEAST(100, GREATEST(0, score + ?)),
                    {$column}  = {$column} + 1,
                    updated_at = NOW()
                ",
                    [$ratedId, $delta, $delta]
                );
            });

            return response()->json(['message' => 'Feedback submitted', 'delta' => $delta], 200);   // sucesso

        } catch (\Illuminate\Validation\ValidationException $e) {
            return response()->json(['message' => 'Validation error', 'errors' => $e->errors()], 422);
        } catch (\Exception $e) {
            \Log::error('giveFeedback error', ['error' => $e->getMessage()]);
            return response()->json(['error' => $e->getMessage()], 500);
        }
    }

    //───────────────────────────────────────────── MOSTRA REPUTAÇAO ────────────────────────────────────────────────────────────────────────────────────
    private function resolveBadges($rep)
    {
        if (!$rep)
            return [];

        $badges = [];

        if ($rep->good_teammate_count >= 5)
            $badges[] = 'Good Teammate';
        if ($rep->friendly_count >= 5)
            $badges[] = 'Friendly Player';
        if ($rep->team_player_count >= 5)
            $badges[] = 'Team Player';

        if ($rep->toxic_count >= 5)
            $badges[] = 'Watchlisted';
        if ($rep->afk_count >= 3)
            $badges[] = 'Frequent No-show';
        if ($rep->bad_sport_count >= 3)
            $badges[] = 'Bad Sport';

        if ($rep->score >= 90)
            $badges[] = 'Elite Reputation';
        if ($rep->score <= 40)
            $badges[] = 'Needs Improvement';

        return $badges;
    }

    public function showReputation($id)
    {
        $rep = \App\Models\UserReputation::find($id);  // obtem o registo de reputaçao na pase de dados

        $data = [   // inicialisa o behaviour index (default 70)
            'user_id' => $id,
            'score' => $rep->score ?? 70,
        ];

        if ($rep) {   // se existir adiciona os feedbaks recebidos
            $data += $rep->only([
                'good_teammate_count',
                'friendly_count',
                'team_player_count',
                'toxic_count',
                'bad_sport_count',
                'afk_count',
            ]);

            $data['badges'] = $this->resolveBadges($rep);
        }

        return response()->json($data, 200);   // sucesso
    }


    //────────────────────────────────────token───────────────────────────────────────────────────────────────

    private function isTokenBlacklisted($token)
    {
        $cacheKey = "blacklisted:{$token}";

        return Cache::has($cacheKey);
    }

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


    private function validateToken(Request $request)
    {
        $token = $this->getTokenFromRequest($request);

        if ($this->isTokenBlacklisted($token)) {
            throw new \Exception('Unauthorized: Token is blacklisted.');
        }

        return $token;
    }









    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────


    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────

    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────


    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────




    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────


    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────

    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────


    //────────────────────────────────────obselete────────────────────────────────────────────────────────────── //────────────────────────────────────obselete──────────────────────────────────────────────────────────────
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
                \Log::warning('Unauthorized attempt to fetch messages for an event:', [
                    'user_id' => $userId,
                    'event_id' => $id,
                ]);
                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            // Fetch all messages from the event_user table for the event
            $messages = EventUser::where('event_id', $id)->get();

            return response()->json(['messages' => $messages], 200);
        } catch (\Exception $e) {
            \Log::error('Error in fetchMessages:', ['error' => $e->getMessage()]);
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    /**
     * Return the list of events that have been concluded
     * and in which the authenticated user took part.
     *
     * GET /concludedEvents
     */
    public function listConcludedEvents(Request $request)
    {
        try {
            // ─── Auth ────────────────────────────────────────────────────────────────
            $token = $this->validateToken($request);
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            // ─── Pull distinct “‑ concluded” records for this user ───────────────────
            $concluded = EventUser::where('user_id', $userId)
                ->where('message', 'like', '%- concluded')   // stored by the consumer
                ->orderByDesc('created_at')
                ->get(['event_id', 'event_name', 'created_at'])
                ->unique('event_id')
                ->values();                                  // re‑index for clean JSON

            return response()->json(['concluded_events' => $concluded], 200);

        } catch (\Exception $e) {
            \Log::error('Error in listConcludedEvents:', ['error' => $e->getMessage()]);
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    /**
     * Let a participant rate another participant (1‑5) after the event is concluded.
     *
     * POST /events/{event_id}/rate
     * Body: { "user_id": 123, "rating": 4 }
     */
    public function rateUser(Request $request, $event_id)
    {
        try {
            // ─── Auth & input ────────────────────────────────────────────────────
            $token = $this->validateToken($request);
            $raterId = JWTAuth::setToken($token)->getPayload()->get('sub');

            $validated = $request->validate([
                'user_id' => 'required|integer',
                'rating' => 'required|integer|min:1|max:5',
            ]);
            $ratedId = (int) $validated['user_id'];

            if ($ratedId === (int) $raterId) {
                return response()->json(['error' => 'You cannot rate yourself'], 400);
            }

            // ─── Event must be concluded ────────────────────────────────────────
            $isConcluded = EventUser::where('event_id', $event_id)
                ->where('message', 'like', '%- concluded')
                ->exists();

            if (!$isConcluded) {
                return response()->json(['error' => 'Event is not concluded yet'], 400);
            }

            // ─── Both users must have participated ──────────────────────────────
            $participants = EventUser::where('event_id', $event_id)
                ->whereIn('user_id', [$raterId, $ratedId])
                ->pluck('user_id')
                ->all();

            if (!in_array($raterId, $participants) || !in_array($ratedId, $participants)) {
                return response()->json(['error' => 'Both users must be participants of this event'], 403);
            }

            // ─── Prevent double‑rating ──────────────────────────────────────────
            $alreadyRated = EventRating::where([
                'event_id' => $event_id,
                'rater_id' => $raterId,
                'rated_id' => $ratedId,
            ])->exists();

            if ($alreadyRated) {
                return response()->json(['error' => 'You have already rated this user for this event'], 400);
            }

            // ─── Store individual rating ───────────────────────────────────────
            EventRating::create([
                'event_id' => $event_id,
                'rater_id' => $raterId,
                'rated_id' => $ratedId,
                'rating' => $validated['rating'],
            ]);

            // ─── Recalculate per‑event average ─────────────────────────────────
            $eventAvg = EventRating::where('event_id', $event_id)
                ->where('rated_id', $ratedId)
                ->avg('rating');

            EventUser::where('event_id', $event_id)
                ->where('user_id', $ratedId)
                ->update(['rating' => $eventAvg]);

            // ─── Recalculate GLOBAL average in user_averages table ─────────────
            $stats = EventRating::selectRaw('COUNT(*) as cnt, AVG(rating) as avg')
                ->where('rated_id', $ratedId)
                ->first();

            UserAverage::updateOrCreate(
                ['user_id' => $ratedId],
                ['average' => $stats->avg, 'ratings_count' => $stats->cnt]
            );

            return response()->json(['message' => 'Rating submitted'], 200);

        } catch (\Illuminate\Validation\ValidationException $e) {
            return response()->json([
                'message' => 'Validation error',
                'errors' => $e->errors(),
            ], 422);
        } catch (\Exception $e) {
            \Log::error('Error in rateUser:', ['error' => $e->getMessage()]);
            return response()->json(['error' => $e->getMessage()], 500);
        }
    }

    /**
     * Return a user’s global average rating.
     *
     * GET /userAverage/{id}
     */
    public function showUserAverage($id)
    {
        $avg = \App\Models\UserAverage::where('user_id', $id)->first();

        return response()->json([
            'user_id' => $id,
            'average_rating' => $avg->average ?? null,
            'ratings_count' => $avg->ratings_count ?? 0,
        ], 200);
    }

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
                \Log::info('Message received by RabbitMQ listener:', ['message' => $msg->body]);

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
            \Log::error('Error in listenRabbitMQ:', ['error' => $e->getMessage()]);
        }
    }

    /**
     * Listen to the event_concluded queue and persist
     * “{event name} – concluded” into event_user.
     *
     * This mirrors listenRabbitMQ() but uses a different queue
     * and rewrites the message before saving.
     */
    public function listenEventConcludedQueue()
    {
        $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
        $queueName = 'rating_event_concluded';            // <── new queue

        try {
            $connection = new AMQPStreamConnection($rabbitmqHost, $rabbitmqPort, $rabbitmqUser, $rabbitmqPassword);
            $channel = $connection->channel();
            $channel->queue_declare($queueName, false, true, false, false);
            $channel->basic_qos(null, 1, null);

            $callback = function (AMQPMessage $msg) {
                $data = json_decode($msg->body, true);

                // We want to display:  “<event name> – concluded”
                $displayText = ($data['event_name'] ?? 'Event') . ' - concluded';

                EventUser::create([
                    'event_id' => $data['event_id'],
                    'event_name' => $data['event_name'],
                    'user_id' => $data['user_id'] ?? null,
                    'user_name' => $data['user_name'] ?? null,
                    'message' => $displayText,
                ]);

                \Log::info('rating_event_concluded consumed', $data);
                $msg->ack();
            };

            $channel->basic_consume($queueName, '', false, false, false, false, $callback);

            while ($channel->is_consuming()) {
                $channel->wait();
            }

            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
            \Log::error('Error in listenEventConcludedQueue:', ['error' => $e->getMessage()]);
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
                \Log::warning('Unauthorized attempt to send a message to an event:', [
                    'user_id' => $userId,
                    'event_id' => $id,
                ]);
                return response()->json(['error' => 'Unauthorized: User is not participating in this event'], 403);
            }

            // Store the message in the event_user table
            $messageData = [
                'event_id' => $id,
                'event_name' => "Evento $id",
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => $validatedData['message'],
            ];

            EventUser::create($messageData);

            // Fetch all unique participants of the event
            $eventParticipants = EventUser::where('event_id', $id)
                ->distinct()
                ->get(['user_id', 'user_name']);

            // Prepare the notification message with distinct participants
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

            // Publish the message to the RabbitMQ queue named 'notification'
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['status' => 'Message sent successfully'], 201);
        } catch (\Illuminate\Validation\ValidationException $e) {
            return response()->json([
                'message' => 'Validation Error',
                'errors' => $e->errors(),
            ], 422);
        } catch (\Exception $e) {
            \Log::error('Error in sendMessage:', ['error' => $e->getMessage(), 'trace' => $e->getTraceAsString()]);
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

            \Log::info('Message published to RabbitMQ:', ['queue' => $queueName, 'message' => $messageBody]);
        } catch (\Exception $e) {
            \Log::error('Error publishing message to RabbitMQ:', ['error' => $e->getMessage(), 'trace' => $e->getTraceAsString()]);
        }
    }

}
