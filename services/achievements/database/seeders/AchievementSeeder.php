<?php  // database/seeders/AchievementSeeder.php
namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Achievement;

class AchievementSeeder extends Seeder
{
  public function run(): void
    {
        $rows = [
            // code,          name,        desc,                                 icon
            ['first_event',  '1 Event',   'Complete your 1st event',            'achievements/1game.png'],
            ['two_events',   '2 Events',  'Complete 2 events',                  'achievements/2game.png'],
            ['three_events', '3 Events',  'Complete 3 events',                  'achievements/3game.png'],
            ['four_events',  '4 Events',  'Complete 4 events',                  'achievements/4game.png'],
            ['five_events',  '5 Events',  'Complete 5 events',                  'achievements/5game.png'],

            // (se quiseres manter os de football, põe também o 4.º campo “icon”)
            // ['first_football_event',  'Football Rookie',      'Complete your 1st football event',  'achievements/fball1.png'],
            // ['second_football_event', 'Football Enthusiast',  'Complete 2 football events',        'achievements/fball2.png'],
        ];

        foreach ($rows as [$code, $name, $desc, $icon]) {
            Achievement::updateOrCreate(
                ['code' => $code],
                ['name' => $name, 'description' => $desc, 'icon' => $icon]
            );
        }
    }
}