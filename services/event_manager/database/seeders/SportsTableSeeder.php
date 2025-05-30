<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;

class SportsTableSeeder extends Seeder
{
    public function run()
    {
           DB::table('sports')->insert([
            ['name' => 'Football'],
            ['name' => 'Futsal'],
            ['name' => 'Cycling'],
            ['name' => 'Surf'],
            ['name' => 'Volleyball'],
            ['name' => 'Basketball'],
            ['name' => 'Tennis'],
            ['name' => 'Handball'],
        ]);
        
    }
}
