<?php

namespace App\Console\Commands;

use Illuminate\Console\Command;
use App\Http\Controllers\ChatController;

class ConsumeEventConcluded extends Command
{
    protected $signature   = 'rabbitmq:consume-event-concluded';
    protected $description = 'Consume messages from the event_concluded queue';

    public function handle(ChatController $chatController)
    {
        $this->info(' [*] Waiting for messages in event_concluded. To exit press CTRL+C');
        $chatController->listenEventConcludedQueue();
        return 0;
    }
}
