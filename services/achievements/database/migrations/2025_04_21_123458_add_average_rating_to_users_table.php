<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        if (!Schema::hasColumn('users', 'average_rating')) {
            Schema::table('users', function (Blueprint $table) {
                $table->decimal('average_rating', 3, 2)
                      ->nullable()
                      ->default(null)
                      ->after('email')
                      ->comment('Mean of all stars received (1‑5)');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasColumn('users', 'average_rating')) {
            Schema::table('users', function (Blueprint $table) {
                $table->dropColumn('average_rating');
            });
        }
    }
};
