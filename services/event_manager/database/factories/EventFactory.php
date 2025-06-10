<?php

namespace Database\Factories;

use App\Models\Event;
use App\Models\Sport;
use Illuminate\Database\Eloquent\Factories\Factory;

class EventFactory extends Factory
{
    protected $model = Event::class;

    public function definition(): array
    {
        // pick an existing sport (or create one on the fly if none yet)
        $sportId = Sport::inRandomOrder()->value('id')
                 ?? Sport::factory()->create()->id;

        // fake creator
        $creatorId   = $this->faker->numberBetween(1, 50);
        $creatorName = "User {$creatorId}";

        // PT bounding box  (≈ mainland Portugal)
        [$lat, $lng] = [
            $this->faker->latitude(36.8, 42.2),
            $this->faker->longitude(-9.5, -6.2),
        ];

        return [
            'name'             => ucfirst($this->faker->words(3, true)),
            'sport_id'         => $sportId,
            'starts_at'        => $this->faker->dateTimeBetween('+1 day', '+3 months'),
            'place'            => $this->faker->city . ', PT',
            'status'           => 'in progress',
            'max_participants' => $this->faker->numberBetween(4, 22),

            // creator → columns added by the 2024_11_30 migration
            'user_id'   => $creatorId,
            'user_name' => $creatorName,

            // geo
            'latitude'  => $lat,
            'longitude' => $lng,

            // leave weather null; it will be fetched live by your controller
            'weather'   => null,
        ];
    }
}
