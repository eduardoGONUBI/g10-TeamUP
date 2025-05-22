<?php

use Illuminate\Database\Migrations\Migration;
use Illuminate\Database\Schema\Blueprint;
use Illuminate\Support\Facades\Schema;

return new class extends Migration
{
    public function up(): void
    {
        Schema::create('event_feedback', function (Blueprint $table) {
            $table->id();

            $table->unsignedBigInteger('event_id');
            $table->unsignedBigInteger('rater_id');   // who gives feedback
            $table->unsignedBigInteger('rated_id');   // who receives it

            // pre‑defined attribute, e.g. good_teammate, toxic …
            $table->string('attribute');

            // +3 for positive, –5 for negative (store actual delta)
            $table->smallInteger('delta');

            $table->timestamps();

            // Only one vote per attribute from same rater to same player in an event
            $table->unique(
                ['event_id', 'rater_id', 'rated_id', 'attribute'],
                'unique_feedback_per_attribute'
            );
        });
    }

    public function down(): void
    {
        Schema::dropIfExists('event_feedback');
    }
};
