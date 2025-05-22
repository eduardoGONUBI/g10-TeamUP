<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class UsersAchi extends Model
{
    protected $table = 'usersachi';

    protected $fillable = [
        'user_id',
        'event_id',
        'event_name',
        'completed_at',
    ];
}
