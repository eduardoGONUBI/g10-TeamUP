<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use App\Models\UserStat;

class UserStatsSeeder extends Seeder
{
    public function run(): void
    {
        // 1) Vai buscar todos os IDs à BD do micro-serviço users
        $userIds = DB::connection('users_db')
                     ->table('users')
                     ->pluck('id');

        // 2) Para cada ID, garante a linha base em user_stats
        foreach ($userIds as $uid) {
            // se já existe, salta — evita duplicar
            if (UserStat::where('user_id', $uid)->exists()) {
                continue;
            }

            // cria com XP/nível definidos pela factory
            UserStat::factory()->create([
                'user_id' => $uid,
            ]);
        }
    }
}
