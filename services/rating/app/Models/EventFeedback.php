<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class EventFeedback extends Model
{
    protected $fillable = [
        'event_id',
        'rater_id',
        'rated_id',
        'attribute',
        'delta',
    ];
}
