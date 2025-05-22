<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class UserReputation extends Model
{
    // primary key is user_id (not auto‑incrementing)
    protected $primaryKey  = 'user_id';
    public    $incrementing = false;

    protected $fillable = [
        'user_id',
        'score',
        'good_teammate_count',
        'friendly_count',
        'team_player_count',
        'toxic_count',
        'bad_sport_count',
        'afk_count',
    ];
}
