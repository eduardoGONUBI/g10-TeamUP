<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Event;

class EventSeeder extends Seeder
{
    public function run()
    {
        // Cria 100 eventos ligados a users reais
        Event::factory()->count(100)->create();
    }
}