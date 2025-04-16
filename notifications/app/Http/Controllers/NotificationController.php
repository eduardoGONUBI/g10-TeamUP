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
     * Listen to RabbitMQ queue and store notifications for all event participants.
     */
    public function listenToNotifications()
    {
        $rabbitmqHost = env('RABBITMQ_HOST', 'rabbitmq');
        $rabbitmqPort = env('RABBITMQ_PORT', 5672);
        $rabbitmqUser = env('RABBITMQ_USER', 'guest');
        $rabbitmqPassword = env('RABBITMQ_PASSWORD', 'guest');
        $queueName = env('RABBITMQ_QUEUE', 'notification');

        try {
            // Connect to RabbitMQ
            $connection = new AMQPStreamConnection($rabbitmqHost, $rabbitmqPort, $rabbitmqUser, $rabbitmqPassword);
            $channel = $connection->channel();

            // Declare the queue
            $channel->queue_declare($queueName, false, true, false, false);

            echo "Waiting for notifications. To exit press CTRL+C\n";

            // Callback for consuming messages
            $callback = function ($msg) {
                echo 'Notification received: ', $msg->body, "\n";

                $messageData = json_decode($msg->body, true);

                if ($messageData && isset($messageData['event_id'], $messageData['participants'], $messageData['event_name'], $messageData['message'])) {
                    try {
                        $initiatorId = $messageData['user_id'] ?? null;

                        foreach ($messageData['participants'] as $participant) {
                            // Skip the initiator (the user who triggered the event)
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

            // Start consuming messages
            $channel->basic_consume($queueName, '', false, false, false, false, $callback);

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
           /* Log::info('Token validated successfully:', ['token' => $token]);*/

            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = (int) $payload->get('sub');
           // Log::info('User ID obtained from token:', ['user_id' => $userId]);

            $notifications = Notification::where('user_id', $userId)
                ->orderBy('created_at', 'desc')
                ->get();

            if ($notifications->isEmpty()) {
              //  Log::info('No notifications found for user:', ['user_id' => $userId]);
                return response()->json(['message' => 'No notifications found'], 404);
            }

            $notificationMessages = $notifications->map(function ($notification) {
                return [
                    'event_name' => $notification->event_name,
                    'message'    => $notification->message,
                    'created_at' => $notification->created_at->toDateTimeString(),
                ];
            })->toArray();

         /*   Log::info('Notifications fetched for user:', [
                'user_id'       => $userId,
                'notifications' => $notificationMessages
            ]);*/

            return response()->json(['notifications' => $notificationMessages], 200);
        } catch (\Exception $e) {
          /*  Log::error('Error fetching notifications:', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);*/
            return response()->json(['error' => 'Failed to fetch notifications', 'details' => $e->getMessage()], 500);
        }
    }

    /**
     * Delete all notifications for the authenticated user.
     */
    public function deleteNotifications(Request $request)
    {
        try {
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = (int) $payload->get('sub');

            // Delete all notifications for this user
            Notification::where('user_id', $userId)->delete();

            return response()->json(['message' => 'All notifications deleted successfully'], 200);
        } catch (\Exception $e) {
         /*   Log::error('Error deleting notifications:', [
                'error' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);*/
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

        // Strip "Bearer " prefix if present
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
