<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        // Add rating only if it is missing
        if (!Schema::hasColumn('event_user', 'rating')) {
            Schema::table('event_user', function (Blueprint $table) {
                $table->tinyInteger('rating')
                      ->after('message')   // or wherever makes sense
                      ->nullable()
                      ->comment('1‑5 rating given after an event is concluded');
            });
        }
    }

    public function down(): void
    {
        if (Schema::hasColumn('event_user', 'rating')) {
            Schema::table('event_user', function (Blueprint $table) {
                $table->dropColumn('rating');
            });
        }
    }
};
