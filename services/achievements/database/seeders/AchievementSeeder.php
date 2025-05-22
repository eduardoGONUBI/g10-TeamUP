<?php  // database/seeders/AchievementSeeder.php
namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Achievement;

class AchievementSeeder extends Seeder
{
    public function run(): void
    {
        $rows = [
            ['first_event',        'First Timer',      'Complete your first event'],
            ['five_events',        'Getting Warm',     'Complete 5 events'],
            ['ten_events',         'Event Veteran',    'Complete 10 events'],
            ['twentyfive_events',  'Marathoner',       'Complete 25 events'],
            ['hundred_events',     'Centurion',        'Complete 100 events'],
            ['soccer_rookie',      'Pitch Rookie',     '3 concluded soccer events'],
            ['soccer_regular',     'Pitch Regular',    '10 concluded soccer events'],
            ['basketball_rookie',  'Court Rookie',     '3 concluded basketball events'],
            ['basketball_regular', 'Court Regular',    '10 concluded basketball events'],
            ['sport_sampler',      'Sport Sampler',    'Play 3 different sports'],
            ['all_rounder',        'All‑rounder',      'Play 5 different sports'],
        ];

        foreach ($rows as $r) {
            Achievement::firstOrCreate(
                ['code' => $r[0]],
                ['name' => $r[1], 'description' => $r[2]]
            );
        }
    }
}