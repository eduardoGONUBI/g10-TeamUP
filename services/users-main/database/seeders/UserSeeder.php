<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\User;
use App\Models\Sport;

class UserSeeder extends Seeder
{
    public function run()
    {
        // Cria 50 utilizadores
        $users = User::factory()->count(50)->create();

        // Associa de 1 a 3 sports aleatórios a cada user
        $sportIds = Sport::pluck('id')->all();

        foreach ($users as $user) {
            $random = array_rand($sportIds, rand(1, 3));
            $toSync = is_array($random)
                ? array_map(fn($i) => $sportIds[$i], $random)
                : [$sportIds[$random]];

            $user->sports()->sync($toSync);
        }
    }
}