<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('user_averages', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('user_id')->unique();
            $table->decimal('average', 3, 2)->default(0);   // 0‑5.00
            $table->integer('ratings_count')->default(0);   // optional
            $table->timestamps();
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('user_averages');
    }
};
