<?php

namespace Database\Seeders;

// use Illuminate\Database\Console\Seeds\WithoutModelEvents;
use Illuminate\Database\Seeder;

class DatabaseSeeder extends Seeder
{
    /**
     * Seed the application's database.
     */
    public function run(): void
    {
        $this->call([
            AdminsTableSeeder::class, // Corrected class name
            SportsTableSeeder::class,
            VerifiedUsersSeeder::class, 
            // Other seeders...
        ]);
    }
}
