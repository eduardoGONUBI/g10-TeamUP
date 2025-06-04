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
        // CASE A: If you already have a DATE-only `date` column, convert it → `starts_at`
        if (Schema::hasColumn('events', 'date')) {
            Schema::table('events', function (Blueprint $table) {
                // 1) Add a temporary nullable DATETIME column called 'starts_at_tmp'
                $table->dateTime('starts_at_tmp')->nullable()->after('date');
            });

            // 2) Copy the old DATE values into 'starts_at_tmp' (append " 00:00:00")
            DB::statement('UPDATE `events` SET `starts_at_tmp` = CONCAT(`date`, " 00:00:00")');

            Schema::table('events', function (Blueprint $table) {
                // 3) Drop the old DATE-only column
                $table->dropColumn('date');
            });

            // 4) Rename the temporary `starts_at_tmp` → `starts_at` (make it NOT NULL if you like)
            DB::statement('ALTER TABLE `events` 
                CHANGE `starts_at_tmp` `starts_at` DATETIME NOT NULL');

            // 5) (Optional) If you want `starts_at` to be strictly NOT NULL, enforce it in schema:
            Schema::table('events', function (Blueprint $table) {
                $table->dateTime('starts_at')->nullable(false)->change();
            });
        }
        // ──────────────────────────────────────────────────────────────

        // CASE B: If there is no `date` column at all (fresh install), simply add `starts_at`.
        if (! Schema::hasColumn('events', 'starts_at')) {
            Schema::table('events', function (Blueprint $table) {
                // 6) Add a new DATETIME `starts_at` that is NOT NULL (or nullable if you prefer)
                $table->dateTime('starts_at')->nullable(false)->after('sport_id');
            });
        }
    }

    /**
     * Reverse the migrations.
     */
    public function down(): void
    {
        // If you have a DATETIME `starts_at` and you want to revert back to a DATE called `date`:
        if (Schema::hasColumn('events', 'starts_at')) {
            // 1) Rename DATETIME `starts_at` → `date_tmp` (still DATETIME for the moment)
            DB::statement('ALTER TABLE `events` CHANGE `starts_at` `date_tmp` DATETIME NULL');

            Schema::table('events', function (Blueprint $table) {
                // 2) Re-create a DATE-only column named `date`
                $table->date('date')->nullable()->after('date_tmp');
            });

            // 3) Copy just YYYY-MM-DD from `date_tmp` → `date`
            DB::statement('UPDATE `events` SET `date` = DATE(`date_tmp`)');

            Schema::table('events', function (Blueprint $table) {
                // 4) Drop the temporary `date_tmp` (which was originally your DATETIME)
                $table->dropColumn('date_tmp');
            });
        }
    }
};
