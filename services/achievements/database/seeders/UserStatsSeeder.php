<?php

namespace Database\Seeders;

use Illuminate\Database\Seeder;
use Illuminate\Support\Facades\DB;
use App\Models\UserStat;

/**
 * Garante que todos os user_ids têm uma linha em user_stats.
 * Define xp e level a 0 se ainda não existir.
 */
class UserStatsSeeder extends Seeder
{
    public function run(): void
    {
        // Se usares **a mesma BD** para os dois serviços
        $ids = DB::table('users')->pluck('id');

        /* -------------------------------------------
         *  Caso cada serviço tenha a sua BD
         *  cria no config/database.php uma ligação
         *  p-ex. 'users_db' e usa:
         *
         *  $ids = DB::connection('users_db')
         *           ->table('users')->pluck('id');
         * ------------------------------------------- */

        foreach ($ids as $id) {
            UserStat::firstOrCreate(
                ['user_id' => $id],
                ['xp' => 0, 'level' => 0]
            );
        }
    }
}
