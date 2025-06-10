<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\User;
use Illuminate\Support\Facades\Hash;
use Illuminate\Support\Str;

class VerifiedUsersSeeder extends Seeder
{
    public function run(): void
    {
        /* -------------------------------------------------------------
         | 1)  the fixed account ‘teste1@gmail.com’
         | ------------------------------------------------------------ */
        $master = User::firstOrCreate(
            ['email' => 'teste1@gmail.com'],
            [
                'name'              => 'teste1',              // must be unique + /^\w+$/
                'password'          => Hash::make('password'),
                'email_verified_at' => now(),
                'location'          => 'Barcelos',
                'remember_token'    => Str::random(10),
            ]
        );

        /* -------------------------------------------------------------
         | 2)  another 49 random, *verified* users
         | ------------------------------------------------------------ */
        User::factory()
            ->count(49)
            ->state(fn () => ['email_verified_at' => now()])
            ->create();
    }
}
