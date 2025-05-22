<?php  // database/migrations/xxxx_xx_xx_add_sport_to_event_user.php
use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration {
    public function up(): void
    {
        Schema::create('user_stats', function (Blueprint $table) {
            $table->unsignedBigInteger('user_id')->primary();
            $table->unsignedBigInteger('xp')->default(0);
            // If you prefer to store only XP and derive the level on-the-fly,
            // drop this next column.  I keep it for quick reads.
            $table->unsignedInteger('level')->nullable();
            $table->timestamps();
        });
    }
    public function down(): void { Schema::dropIfExists('user_stats'); }
};