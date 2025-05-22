<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Participant extends Model
{
    use SoftDeletes;   // ← enables the deleted_at timestamp

    protected $table = 'event_user';

    protected $fillable = [
        'event_id',
        'user_id',
        'user_name',
        'rating',      // keep this if you store per-event ratings here
    ];

    protected $dates = [
        'created_at',
        'updated_at',
        'deleted_at',  // ← helps Carbon cast the soft-delete column
    ];

    public $timestamps = true;

    // Relationship back to the event
    public function event()
    {
        return $this->belongsTo(Event::class);
    }
}
