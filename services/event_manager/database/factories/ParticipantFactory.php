<?php

namespace Database\Factories;

use App\Models\Event;
use App\Models\Participant;
use Illuminate\Database\Eloquent\Factories\Factory;

class ParticipantFactory extends Factory
{
    protected $model = Participant::class;

    public function definition(): array
    {
        $event = Event::inRandomOrder()->first() ?? Event::factory()->create();

        $userId   = $this->faker->numberBetween(1, 50);
        $userName = "User {$userId}";

        return [
            'event_id'   => $event->id,
            'user_id'    => $userId,
            'user_name'  => $userName,
            'rating'     => null,
            'created_at' => now(),
            'updated_at' => now(),
        ];
    }
}
