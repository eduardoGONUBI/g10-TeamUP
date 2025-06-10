<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use App\Models\Event;
use App\Models\User;

class EventUserSeeder extends Seeder
{
    public function run()
    {
        Event::all()->each(function ($event) {
            $ids = User::where('id', '!=', $event->user_id)
                ->inRandomOrder()
                ->take(rand(5, 20))
                ->pluck('id');

            $pivot = $ids->map(fn($id) => [
                'user_id'   => $id,
                'user_name' => User::find($id)->name,
                'created_at'=> now(),
                'updated_at'=> now(),
            ])->all();

            $event->users()->attach($pivot);
        });
    }
}