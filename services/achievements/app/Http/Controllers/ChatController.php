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
use App\Models\Achievement;
use App\Models\UsersAchi;
use App\Models\UserAchievement;
use App\Models\UserStat;

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
    $rabbitmqHost     = env('RABBITMQ_HOST', 'rabbitmq');
    $rabbitmqPort     = env('RABBITMQ_PORT', 5672);
    $rabbitmqUser     = env('RABBITMQ_USER', 'guest');
    $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
    $queueName        = 'event_concluded';

    try {
        $connection = new AMQPStreamConnection($rabbitmqHost, $rabbitmqPort, $rabbitmqUser, $rabbitmqPassword);
        $channel    = $connection->channel();
        $channel->queue_declare($queueName, false, true, false, false);
        $channel->basic_qos(null, 1, null);

        $callback = function (AMQPMessage $msg) {
            try {
                $data = json_decode($msg->body, true);

                // Fallbacks in case the producer forgot fields
                $eventId   = $data['event_id']   ?? null;
                $eventName = $data['event_name'] ?? 'Event';
                $userId    = $data['user_id']    ?? null;
                $userName  = $data['user_name']  ?? null;

                \Log::info('event_concluded raw message', ['data' => $data]);

                // Persist chat feed line: “<event name> – concluded”
                $displayText = "{$eventName} - concluded";

                EventUser::create([
                    'event_id'   => $eventId,
                    'event_name' => $eventName,
                    'user_id'    => $userId,
                    'user_name'  => $userName,
                    'message'    => $displayText,
                ]);

                // ✅ Save to usersachi + unlock for each participant
                if ($eventId) {
                    $participants = \App\Models\EventUser::where('event_id', $eventId)
                        ->select('user_id', 'user_name')
                        ->distinct()
                        ->get();

                    foreach ($participants as $participant) {
                        \App\Models\UsersAchi::firstOrCreate(
                            [
                                'user_id'  => $participant->user_id,
                                'event_id' => $eventId,
                            ],
                            [
                                'event_name'   => $eventName,
                                'completed_at' => now(),
                            ]
                        );

                        $this->resolveUserAchiAchievements($participant->user_id);
                        $this->addXp($participant->user_id, self::EVENT_XP);   // ← NEW

                    }
                }

                \Log::info('event_concluded consumed successfully', $data);
                $msg->ack();
            } catch (\Exception $e) {
                \Log::error('Error processing event_concluded message', [
                    'error' => $e->getMessage(),
                    'trace' => $e->getTraceAsString(),
                    'raw_message' => $msg->body,
                ]);

                $msg->nack(true); // requeue = true
            }
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
 * Return the user’s XP and level.
 * If the user never appeared in any event yet, both fields are null.
 *
 * GET /api/profile/{id}
 */
public function getProfile($id)
{
    $stat = \App\Models\UserStat::find($id);   // null if row doesn’t exist

    return response()->json([
        'xp'    => $stat->xp    ?? null,
        'level' => $stat->level ?? null,
    ], 200);
}



private function resolveUserAchiAchievements(int $userId): void
{
    $count = \App\Models\UsersAchi::where('user_id', $userId)->count();

   foreach ([1=>'first_event',2=>'two_events',3=>'three_events',4=>'four_events',5=>'five_events'] as $threshold=>$code){
        if ($count >= $threshold) {
            $this->unlock($userId, $code);
        }
    }
}


/* -------------------------------------------------------------------------
 |  Helpers – keep inside the same controller (or move to a service class)
 * ---------------------------------------------------------------------- */

/**
 * Check all badge rules that depend on event‑completion counts.
 */
/**
 * Check badge rules for total concluded events only.
 */

/* -------------------------------------------------------------------------
 |  XP / LEVEL CONFIG
 * ---------------------------------------------------------------------- */

 private const EVENT_XP = 50;          // XP per event concluded

 private const ACHIEVEMENT_XP = [      // XP per achievement unlocked
    'first_event'  => 25,
    'two_events'   => 35,
    'three_events' => 45,
    'four_events'  => 55,
    'five_events'  => 65,
];
 
 // cut-offs for levels  (L0, L1, L2…)
 private const LEVEL_CUTS = [0, 25, 60, 120, 200, 300, 450, 650, 900];
 //                      Lvl 0  1   2    3    4    5    6    7    8
 private function levelFor(int $xp): int
 {
     foreach (array_reverse(self::LEVEL_CUTS, true) as $level => $cut) {
         if ($xp >= $cut) return $level;
     }
     return 0;
 }
 
 private function addXp(int $userId, int $xp): void
 {
     if ($xp <= 0) return;
 
     DB::transaction(function () use ($userId, $xp) {
         $row = UserStat::lockForUpdate()->firstOrNew(['user_id' => $userId]);
         $row->xp    += $xp;
         $row->level  = $this->levelFor($row->xp);
         $row->save();
     });
 
     Cache::forget('user_level:' . $userId);   // optional – if you cache levels
 }
 

private function resolveEventAchievements(int $userId): void
{
    $total = EventUser::where('user_id', $userId)
        ->where('message', 'like', '%- concluded')
        ->count();

  $milestones = [
    1=>'first_event', 2=>'two_events', 3=>'three_events',
    4=>'four_events', 5=>'five_events',
];

    foreach ($milestones as $threshold => $code) {
        if ($total >= $threshold) {
            $this->unlock($userId, $code);
        }
    }
}

/**
 * Grant a single achievement code to the user (no duplicates).
 */
private function unlock(int $userId, string $code): void
{
    // ✅ Check user participation in any event
    $isKnownUser = \App\Models\EventUser::where('user_id', $userId)->exists();
    if (!$isKnownUser) {
        \Log::warning("Achievement skipped — user not found in event_user", [
            'user_id' => $userId,
            'code' => $code,
        ]);
        return;
    }

    $ach = Achievement::where('code', $code)->first();
    if (!$ach) {
        \Log::warning("Achievement code not found", ['code' => $code]);
        return;
    }

    $unlocked = UserAchievement::firstOrCreate(
        ['user_id' => $userId, 'achievement_id' => $ach->id],
        ['unlocked_at' => now()]
    );

    if ($unlocked->wasRecentlyCreated) {
        $xp = self::ACHIEVEMENT_XP[$code] ?? 0;
        $this->addXp($userId, $xp);
        \Log::info("🎉 User {$userId} unlocked {$ach->name} (+{$xp} XP)");
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



public function listAchievements($id)
{
    $list = UserAchievement::with('achievement')
        ->where('user_id', $id)
        ->orderBy('unlocked_at')
        ->get()
        ->map(fn ($ua) => [
            'code'        => $ua->achievement->code,
            'title'       => $ua->achievement->name,
            'description' => $ua->achievement->description,
            'icon'        => url($ua->achievement->icon),
            'unlocked_at' => $ua->unlocked_at,
        ]);

    return response()->json(['achievements' => $list], 200);
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
