<?php

namespace App\Models;

use Illuminate\Database\Eloquent\Model;

class UserAverage extends Model
{
    protected $fillable = ['user_id', 'average', 'ratings_count'];
}
