<?php

namespace Database\Factories;

use Illuminate\Database\Eloquent\Factories\Factory;
use App\Models\UserStat;

class UserStatFactory extends Factory
{
    protected $model = UserStat::class;

    private const CUTS = [0, 25, 60, 120, 200, 300, 450, 650, 900];

    private function levelFor(int $xp): int
    {
        foreach (array_reverse(self::CUTS, true) as $lvl => $cut) {
            if ($xp >= $cut) return $lvl;
        }
        return 0;
    }

    public function definition(): array
    {
        $xp = $this->faker->numberBetween(0, 1000);
        return ['xp' => $xp, 'level' => $this->levelFor($xp)];
    }
}
