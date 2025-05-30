<?php  // database/seeders/AchievementSeeder.php
namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Achievement;

class AchievementSeeder extends Seeder
{
    public function run(): void
    {
        $rows = [
            // genéricas de 1–5 eventos
            ['first_event',           '1 Event',               'Complete your 1st event'],
            ['two_events',            '2 Events',              'Complete 2 events'],
            ['three_events',          '3 Events',              'Complete 3 events'],
            ['four_events',           '4 Events',              'Complete 4 events'],
            ['five_events',           '5 Events',              'Complete 5 events'],

            // NOVAS de futebol (football) (NOT WORKING)
            ['first_football_event',  'Football Rookie',       'Complete your 1st football event'],
            ['second_football_event', 'Football Enthusiast',   'Complete 2 football events'],
        ];

       foreach ($rows as [$code, $name, $desc, $icon]) {
    Achievement::updateOrCreate(
        ['code' => $code],
        ['name' => $name, 'description' => $desc, 'icon' => $icon]
    );
        }
    }
}
