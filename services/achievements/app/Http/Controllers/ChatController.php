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
    /* ---------------------------------------------------------------------
     |  Queues – event_joined  (unchanged)
     * ------------------------------------------------------------------ */
 public function listenRabbitMQ()
{
    $rabbitmqHost     = env('RABBITMQ_HOST', 'rabbitmq');
    $rabbitmqPort     = env('RABBITMQ_PORT', 5672);
    $rabbitmqUser     = env('RABBITMQ_USER', 'guest');
    $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');

    // Consumir ambas as filas em que o EventController publica
    $queues = [
        env('RABBITMQ_QUEUE', 'event_joined'),
        'ach_event_join-leave',
    ];

    try {
        $connection = new AMQPStreamConnection(
            $rabbitmqHost,
            $rabbitmqPort,
            $rabbitmqUser,
            $rabbitmqPassword
        );
        $channel = $connection->channel();

        foreach ($queues as $queueName) {
            // Garante que existe e é durável
            $channel->queue_declare($queueName, false, true, false, false);
            $channel->basic_qos(null, 1, null);

            $channel->basic_consume(
                $queueName,
                '',
                false,
                false,
                false,
                false,
                function (AMQPMessage $msg) {
                    \Log::info("Join message received on {$msg->getRoutingKey()}:", ['body' => $msg->body]);
                    $data = json_decode($msg->body, true);

                    // Insere (ou ignora se já existir) na tabela event_user
                    EventUser::firstOrCreate(
                        [
                            'event_id' => $data['event_id'],
                            'user_id'  => $data['user_id'],
                        ],
                        [
                            'event_name' => $data['event_name'],
                            'user_name'  => $data['user_name'],
                            'message'    => $data['message'],
                        ]
                    );

                    $msg->ack();
                }
            );
        }

        while ($channel->is_consuming()) {
            $channel->wait();
        }

        $channel->close();
        $connection->close();
    } catch (\Exception $e) {
        \Log::error('Error in listenRabbitMQ:', ['error' => $e->getMessage()]);
    }
}

    /* ---------------------------------------------------------------------
     |  Queue – event_concluded  (unchanged except milestone logic)
     * ------------------------------------------------------------------ */
    public function listenEventConcludedQueue()
    {
        $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
        $queueName = 'event_concluded';

        try {
            $connection = new AMQPStreamConnection($rabbitmqHost, $rabbitmqPort, $rabbitmqUser, $rabbitmqPassword);
            $channel    = $connection->channel();
            $channel->queue_declare($queueName, false, true, false, false);
            $channel->basic_qos(null, 1, null);

            $callback = function (AMQPMessage $msg) {
                try {
                    $data = json_decode($msg->body, true);

                    $eventId   = $data['event_id']   ?? null;
                    $eventName = $data['event_name'] ?? 'Event';
                    $userId    = $data['user_id']    ?? null;
                    $userName  = $data['user_name']  ?? null;

                    \Log::info('event_concluded raw message', ['data' => $data]);

                    $displayText = "{$eventName} - concluded";

                    EventUser::create([
                        'event_id'   => $eventId,
                        'event_name' => $eventName,
                        'user_id'    => $userId,
                        'user_name'  => $userName,
                        'message'    => $displayText,
                    ]);

                    if ($eventId) {
                        $participants = EventUser::where('event_id', $eventId)
                            ->select('user_id', 'user_name')
                            ->distinct()
                            ->get();

                        foreach ($participants as $p) {
                            UsersAchi::firstOrCreate(
                                ['user_id' => $p->user_id, 'event_id' => $eventId],
                                ['event_name' => $eventName, 'completed_at' => now()]
                            );

                            $this->resolveUserAchiAchievements($p->user_id);
                            $this->addXp($p->user_id, self::EVENT_XP);
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
                    $msg->nack(true);
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

/* -----------------------------------------------------------------
 |  Public profile endpoint  – self–healing
 * ----------------------------------------------------------------*/
public function getProfile($id)
{
    // if the user never earned XP we create a blank record on the fly
    $stat = \App\Models\UserStat::firstOrCreate(
        ['user_id' => $id],
        ['xp' => 0, 'level' => 0]
    );

    return response()->json([
        'xp'    => (int) $stat->xp,
        'level' => (int) $stat->level,
    ], 200);
}

    /* ---------------------------------------------------------------------
     |  Achievement resolution – UPDATED (1..5 events)
     * ------------------------------------------------------------------ */
    private function resolveUserAchiAchievements(int $userId): void
    {
        $count = UsersAchi::where('user_id', $userId)->count();

        foreach ([1=>'first_event',2=>'two_events',3=>'three_events',4=>'four_events',5=>'five_events'] as $t=>$code) {
            if ($count >= $t) {
                $this->unlock($userId, $code);
            }
        }
    }

    private function resolveEventAchievements(int $userId): void
    {
        $total = EventUser::where('user_id', $userId)
            ->where('message', 'like', '%- concluded')
            ->count();

        $milestones = [
            1=>'first_event',2=>'two_events',3=>'three_events',
            4=>'four_events',5=>'five_events',
        ];

        foreach ($milestones as $t=>$code) {
            if ($total >= $t) {
                $this->unlock($userId, $code);
            }
        }
    }

    public function leaderboard(Request $req)
{
    // how many per page?
    $perPage = $req->query('per_page', 15);

    // base query on the exact same model as getProfile
    $qb = UserStat::query()
        ->orderByDesc('level')
        ->orderByDesc('xp');

    // Laravel will auto‐read ?page= from the query string
    $pageData = $qb->paginate($perPage);

    // compute a 1‐based overall rank for each row in this page
    $startRank = ($pageData->currentPage() - 1) * $pageData->perPage();
    $rows = $pageData->getCollection()->map(function($stat, $idx) use ($startRank) {
        return [
            'rank'    => $startRank + $idx + 1,
            'user_id' => $stat->user_id,
            'level'   => (int)$stat->level,
            'xp'      => (int)$stat->xp,
        ];
    });

    return response()->json([
        'data' => $rows,
        'meta' => [
            'current_page' => $pageData->currentPage(),
            'last_page'    => $pageData->lastPage(),
            'per_page'     => $pageData->perPage(),
            'total'        => $pageData->total(),
        ],
    ], 200);
}
    /* ---------------------------------------------------------------------
     |  XP / Levels – constants  (ACHIEVEMENT_XP updated)
     * ------------------------------------------------------------------ */
    private const EVENT_XP = 25;

    private const ACHIEVEMENT_XP = [
        'first_event'  => 25,
        'two_events'   => 35,
        'three_events' => 45,
        'four_events'  => 55,
        'five_events'  => 65,
    ];

    private const LEVEL_CUTS = [0, 25, 60, 120, 200, 300, 450, 650, 900];

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

        Cache::forget('user_level:' . $userId);
    }

    /* ---------------------------------------------------------------------
     |  Unlock helper  (unchanged)
     * ------------------------------------------------------------------ */
    private function unlock(int $userId, string $code): void
    {
        $exists = EventUser::where('user_id', $userId)->exists();
        if (!$exists) {
            \Log::warning("Achievement skipped — user not found in event_user", [
                'user_id' => $userId, 'code' => $code,
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

    /* ---------------------------------------------------------------------
     |  Public listAchievements endpoint  (icon → full URL)
     * ------------------------------------------------------------------ */
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
                'icon' => config('app.url') . '/' . ltrim($ua->achievement->icon, '/'),
                'unlocked_at' => $ua->unlocked_at,
            ]);

        return response()->json(['achievements' => $list], 200);
    }

    /* ---------------------------------------------------------------------
     |  Helpers – token & queue (unchanged)
     * ------------------------------------------------------------------ */
    private function isTokenBlacklisted($token)
    {
        return Cache::has("blacklisted:{$token}");
    }

    private function getTokenFromRequest(Request $request)
    {
        $token = $request->header('Authorization') ?? '';
        if (str_starts_with($token, 'Bearer ')) {
            $token = substr($token, 7);
        }
        if (!$token) {
            throw new \Exception('Token is required.');
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

    private function publishToRabbitMQ($queueName, $messageBody)
    {
        $host = env('RABBITMQ_HOST', 'rabbitmq');
        $port = env('RABBITMQ_PORT', 5672);
        $user = env('RABBITMQ_USER', 'guest');
        $pass = env('RABBITMQ_PASSWORD', 'guest');

        try {
            $conn = new AMQPStreamConnection($host, $port, $user, $pass);
            $ch   = $conn->channel();
            $ch->queue_declare($queueName, false, true, false, false);

            $ch->basic_publish(new AMQPMessage($messageBody), '', $queueName);

            $ch->close();
            $conn->close();
        } catch (\Exception $e) {
            \Log::error('Error publishing message to RabbitMQ:', [
                'queue' => $queueName, 'error' => $e->getMessage()
            ]);
        }
    }
}
