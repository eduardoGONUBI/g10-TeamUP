<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;
use Illuminate\Support\Facades\DB;

return new class extends Migration
{
    /**
     * Run the migrations.
     */
    public function up(): void
    {
        // If the old 'date' column still exists, we need to convert it to DATETIME:
        if (Schema::hasColumn('events', 'date')) {
            Schema::table('events', function (Blueprint $table) {
                // 1) Add a temporary nullable DATETIME column named 'starts_at'
                $table->dateTime('starts_at')->nullable()->after('date');
            });

            // 2) Copy old DATE values into 'starts_at' by appending " 00:00:00"
            DB::statement('UPDATE `events` SET `starts_at` = CONCAT(`date`, " 00:00:00")');

            // 3) Drop the old DATE-only column
            Schema::table('events', function (Blueprint $table) {
                $table->dropColumn('date');
            });

            // 4) Rename 'starts_at' → 'date' (as DATETIME NOT NULL)
            // Use MySQL's CHANGE clause because some MySQL versions do not support RENAME COLUMN
            DB::statement('ALTER TABLE `events` CHANGE `starts_at` `date` DATETIME NOT NULL');

            // 5) Enforce NOT NULL at the schema level (no-op if the previous statement already did it)
            Schema::table('events', function (Blueprint $table) {
                $table->dateTime('date')->nullable(false)->change();
            });
        }

        // If `date` column does not exist, do nothing.
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        // If the current column is a DATETIME named 'date', revert it back to DATE:
        if (Schema::hasColumn('events', 'date')) {
            // 1) Rename DATETIME `date` → `starts_at` (allow NULL for now)
            DB::statement('ALTER TABLE `events` CHANGE `date` `starts_at` DATETIME NULL');

            // 2) Add back a DATE-only column named `date`
            Schema::table('events', function (Blueprint $table) {
                $table->date('date')->nullable()->after('starts_at');
            });

            // 3) Copy just the YYYY-MM-DD portion back into `date`
            DB::statement('UPDATE `events` SET `date` = DATE(`starts_at`)');

            // 4) Drop the temporary `starts_at`
            Schema::table('events', function (Blueprint $table) {
                $table->dropColumn('starts_at');
            });
        }
    }
};
