<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use App\Models\Event;
use App\Models\Participant;
use App\Models\Sport;

class EventWithParticipantsSeeder extends Seeder
{
    public function run(): void
    {
        // 1) Fetch real users
        $response = Http::get(config('services.users.base_uri') . '/api/internal/users-list');
        if (! $response->ok()) {
            Log::error('Could not fetch users for seeding events');
            return;
        }

        $users = collect($response->json());
        if ($users->isEmpty()) {
            Log::warning('No users received, skipping event seeding');
            return;
        }

        $pickRandomUser = fn () => $users->random();
        $specialUser    = $users->firstWhere('email', 'teste1@gmail.com') ?? $users->first();
        $sportIds       = Sport::pluck('id');

        // 2) Create 20 events by the special user
        for ($i = 0; $i < 20; $i++) {
            $this->createEventWithParticipants($specialUser, $sportIds, $users);
        }

        // 3) Create 60 random events
        for ($i = 0; $i < 60; $i++) {
            $this->createEventWithParticipants($pickRandomUser(), $sportIds, $users);
        }

        // 4) Create 20 events specifically in the Barcelos area
        for ($i = 0; $i < 20; $i++) {
            $creator = $pickRandomUser();
            // Barcelos bounding box
            $lat = fake()->latitude(41.5, 41.6);
            $lng = fake()->longitude(-8.7, -8.5);
            $event = Event::create([
                'name'             => ucfirst(fake()->words(3, true)),
                'sport_id'         => $sportIds->random(),
                'starts_at'        => fake()->dateTimeBetween('+1 day', '+3 months'),
                'place'            => fake()->streetName . ', Barcelos, PT',
                'status'           => 'in progress',
                'max_participants' => fake()->numberBetween(4, 22),
                'user_id'          => $creator['id'],
                'user_name'        => $creator['name'],
                'latitude'         => $lat,
                'longitude'        => $lng,
            ]);

            // add creator as participant
            Participant::create([
                'event_id'  => $event->id,
                'user_id'   => $creator['id'],
                'user_name' => $creator['name'],
            ]);

            // extra random participants
            $slots = max(0, $event->max_participants - 1);
            $extra = fake()->numberBetween(min(1, $slots), min(10, $slots));
            $ids   = $users->where('id', '!=', $creator['id'])->shuffle()->take($extra);
            foreach ($ids as $u) {
                Participant::create([
                    'event_id'  => $event->id,
                    'user_id'   => $u['id'],
                    'user_name' => $u['name'],
                ]);
            }
        }
    }

    private function createEventWithParticipants(array $creator, $sportIds, $users): void
    {
        // Fake geo coords roughly inside Portugal
        $lat = fake()->latitude(36.8, 42.2);
        $lng = fake()->longitude(-9.5, -6.2);

        $event = Event::create([
            'name'             => ucfirst(fake()->words(3, true)),
            'sport_id'         => $sportIds->random(),
            'starts_at'        => fake()->dateTimeBetween('+1 day', '+3 months'),
            'place'            => fake()->city . ', PT',
            'status'           => 'in progress',
            'max_participants' => fake()->numberBetween(4, 22),
            'user_id'          => $creator['id'],
            'user_name'        => $creator['name'],
            'latitude'         => $lat,
            'longitude'        => $lng,
        ]);

        // creator as participant
        Participant::create([
            'event_id'  => $event->id,
            'user_id'   => $creator['id'],
            'user_name' => $creator['name'],
        ]);

        // extra random participants
        $slots = max(0, $event->max_participants - 1);
        $extra = fake()->numberBetween(min(1, $slots), min(10, $slots));
        $ids   = $users->where('id', '!=', $creator['id'])->shuffle()->take($extra);
        foreach ($ids as $u) {
            Participant::create([
                'event_id'  => $event->id,
                'user_id'   => $u['id'],
                'user_name' => $u['name'],
            ]);
        }
    }
}
