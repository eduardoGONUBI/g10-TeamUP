<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('event_ratings', function (Blueprint $table) {
            $table->id();
            $table->unsignedBigInteger('event_id');
            $table->unsignedBigInteger('rater_id');   // who gives the rating
            $table->unsignedBigInteger('rated_id');   // who receives it
            $table->tinyInteger('rating');            // 1‑5
            $table->timestamps();

            $table->unique(['event_id', 'rater_id', 'rated_id'],
                           'unique_per_event_per_pair');

            // (optional) add FKs if you wish
            // $table->foreign('event_id')->references('id')->on('events');
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('event_ratings');
    }
};
