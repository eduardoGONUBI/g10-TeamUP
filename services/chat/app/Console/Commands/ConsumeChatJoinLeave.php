<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use PhpAmqpLib\Connection\AMQPStreamConnection;
use PhpAmqpLib\Message\AMQPMessage;
use Illuminate\Support\Facades\Log;
use App\Models\EventUser;

class ConsumeChatJoinLeave extends Command
{
    protected $signature = 'rabbitmq:consume-lolchat-join-leave';
    protected $description = 'Consume lolchat_event_join-leave messages from RabbitMQ and store them in event_user table';

    public function handle()
    {
        $this->info('Listening to lolchat_event_join-leave queue...');

        try {
            $connection = new AMQPStreamConnection('rabbitmq', 5672, 'guest', 'guest');
            $channel = $connection->channel();

            $queueName = 'lolchat_event_join-leave';
            $channel->queue_declare($queueName, false, true, false, false);

            $callback = function (AMQPMessage $msg) {
                $data = json_decode($msg->body, true);

                if (!isset($data['event_id'], $data['user_id'], $data['user_name'], $data['action'])) {
                    Log::warning("Invalid payload received", ['body' => $msg->body]);
                    $msg->ack();
                    return;
                }

                $message = "User {$data['user_name']} {$data['action']} the event";

                EventUser::create([
                    'event_id'   => $data['event_id'],
                    'event_name' => $data['event_name'] ?? 'Evento ' . $data['event_id'],
                    'user_id'    => $data['user_id'],
                    'user_name'  => $data['user_name'],
                    'message'    => $message,
                ]);

                Log::info("Stored join/leave event", ['event_id' => $data['event_id'], 'user_id' => $data['user_id'], 'action' => $data['action']]);
                $this->info("Stored: {$message}");

                $msg->ack();
            };

            $channel->basic_consume($queueName, '', false, false, false, false, $callback);

            while ($channel->is_consuming()) {
                $channel->wait();
            }

            $channel->close();
            $connection->close();
        } catch (\Exception $e) {
            $this->error("RabbitMQ consumption failed: " . $e->getMessage());
            Log::error("RabbitMQ consumption error", ['exception' => $e]);
        }
    }
}
