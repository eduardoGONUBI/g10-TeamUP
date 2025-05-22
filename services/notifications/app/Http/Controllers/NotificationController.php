<?php

namespace App\Http\Controllers;

use App\Models\Notification;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Cache;
use Illuminate\Support\Facades\Log;
use Tymon\JWTAuth\Facades\JWTAuth;
use PhpAmqpLib\Connection\AMQPStreamConnection;

class NotificationController extends Controller
{
    /**
     * Listen to RabbitMQ fanout exchange and store notifications for all event participants.
     */
    public function listenToNotifications()
    {
        $rabbitmqHost     = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort     = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser     = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');

        // Hardcoded exchange to consume (the "other" fanout)
        $exchangeName = 'notification_fanout_2';

        try {
            // Connect to RabbitMQ
            $connection = new AMQPStreamConnection(
                $rabbitmqHost,
                $rabbitmqPort,
                $rabbitmqUser,
                $rabbitmqPassword
            );
            $channel = $connection->channel();

            // Declare the fanout exchange
            $channel->exchange_declare(
                $exchangeName,  // name
                'fanout',       // type
                false,          // passive
                true,           // durable
                false           // auto_delete
            );

            // Create a temporary, exclusive queue
            list($queueName,,) = $channel->queue_declare(
                '',      // let server generate a random queue name
                false,   // passive
                true,    // durable
                false,   // exclusive
                true     // auto_delete
            );

            // Bind the queue to the exchange
            $channel->queue_bind($queueName, $exchangeName);

            echo "Waiting for notifications in exchange '{$exchangeName}'. To exit press CTRL+C\n";

            // Callback for consuming messages
            $callback = function ($msg) {
                echo 'Notification received: ', $msg->body, "\n";

                $messageData = json_decode($msg->body, true);

                if ($messageData && isset(
                    $messageData['event_id'],
                    $messageData['participants'],
                    $messageData['event_name'],
                    $messageData['message']
                )) {
                    try {
                        $initiatorId = $messageData['user_id'] ?? null;

                        foreach ($messageData['participants'] as $participant) {
                            // Skip the initiator
                            if ((int)$participant['user_id'] === (int)$initiatorId) {
                                continue;
                            }

                            Notification::create([
                                'event_id'   => $messageData['event_id'],
                                'user_id'    => $participant['user_id'],
                                'event_name' => $messageData['event_name'],
                                'message'    => $messageData['message'],
                            ]);

                            echo "Notification saved for user_id: " . $participant['user_id'] . "\n";
                        }
                    } catch (\Exception $e) {
                        echo 'Error saving notification: ' . $e->getMessage() . "\n";
                    }
                } else {
                    echo 'Received invalid message format. Missing required fields.' . "\n";
                }

                // Acknowledge message
                $msg->ack();
            };

            // Start consuming from the bound queue
            $channel->basic_consume(
                $queueName,
                '',
                false,
                false,
                false,
                false,
                $callback
            );

            // loop
            while ($channel->is_consuming()) {
                $channel->wait();
            }

            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
            echo 'Error in listenToNotifications: ' . $e->getMessage() . "\n";
        }
    }

    /**
     * Get all notifications for the authenticated user.
     */
    public function getNotifications(Request $request)
    {
        try {
            $token = $this->validateToken($request);
            Log::info('Token validated successfully:', ['token' => $token]);

            $payload     = JWTAuth::setToken($token)->getPayload();
            $userId      = (int)$payload->get('sub');
            $notifications = Notification::where('user_id', $userId)
                ->orderBy('created_at', 'desc')
                ->get();

            if ($notifications->isEmpty()) {
                Log::info('No notifications found for user:', ['user_id' => $userId]);
                return response()->json(['message' => 'No notifications found'], 404);
            }

            $notificationMessages = $notifications->map(function ($notification) {
                return [
                    'event_name' => $notification->event_name,
                    'message'    => $notification->message,
                    'created_at' => $notification->created_at->toDateTimeString(),
                ];
            })->toArray();

            Log::info('Notifications fetched for user:', [
                'user_id'       => $userId,
                'notifications' => $notificationMessages
            ]);

            return response()->json(['notifications' => $notificationMessages], 200);
        } catch (\Exception $e) {
            Log::error('Error fetching notifications:', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);
            return response()->json(['error' => 'Failed to fetch notifications', 'details' => $e->getMessage()], 500);
        }
    }

    /**
     * Delete all notifications for the authenticated user.
     */
    public function deleteNotifications(Request $request)
    {
        try {
            $token   = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId  = (int)$payload->get('sub');

            Notification::where('user_id', $userId)->delete();

            return response()->json(['message' => 'All notifications deleted successfully'], 200);
        } catch (\Exception $e) {
            Log::error('Error deleting notifications:', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);
            return response()->json(['error' => 'Failed to delete notifications', 'details' => $e->getMessage()], 500);
        }
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

    /**
     * Extract token from Authorization header.
     */
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

    /**
     * Check if a token is blacklisted.
     */
    private function isTokenBlacklisted($token)
    {
        $cacheKey = "blacklisted:{$token}";
        return Cache::has($cacheKey);
    }
}
