<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('user_reputations', function (Blueprint $table) {
            // PK is also the FK to users.id
            $table->unsignedBigInteger('user_id')->primary();
            $table->integer('score')->default(70);          // 0‑100 scale, starts at 70
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('user_reputations');
    }
};
