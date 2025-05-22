<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class UserStat extends Model
{
    protected $fillable = ['user_id','xp','level'];
    public $incrementing = false;          // user_id is the PK
    protected $primaryKey = 'user_id';
    protected $casts = [
        'xp'    => 'integer',
        'level' => 'integer',
    ];
}