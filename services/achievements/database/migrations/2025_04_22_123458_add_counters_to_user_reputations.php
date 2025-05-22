<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::table('user_reputations', function (Blueprint $table) {
            foreach ([
                'good_teammate_count',
                'friendly_count',
                'team_player_count',
                'toxic_count',
                'bad_sport_count',
                'afk_count',
            ] as $col) {
                if (!Schema::hasColumn('user_reputations', $col)) {
                    $table->integer($col)->default(0)->after('score');
                }
            }
        });
    }

    public function down(): void
    {
        Schema::table('user_reputations', function (Blueprint $table) {
            $table->dropColumn([
                'good_teammate_count',
                'friendly_count',
                'team_player_count',
                'toxic_count',
                'bad_sport_count',
                'afk_count',
            ]);
        });
    }
};
