<?php

namespace App\Models;

use Illuminate\Contracts\Auth\MustVerifyEmail;
use Illuminate\Database\Eloquent\Factories\HasFactory;
use Illuminate\Foundation\Auth\User as Authenticatable;
use Illuminate\Notifications\Notifiable;
use Illuminate\Support\Facades\Storage;
use PHPOpenSourceSaver\JWTAuth\Contracts\JWTSubject;
use App\Notifications\VerifyEmail;

class User extends Authenticatable implements JWTSubject, MustVerifyEmail
{
    use HasFactory, Notifiable;

    /**
     * Mass-assignable attributes.
     */
    protected $fillable = [
        'name',
        'email',
        'password',
        'sport',
        'location',
        'avatar_url',          // ← new
    ];

    /**
     * Hidden attributes for arrays / JSON.
     */
    protected $hidden = [
        'password',
        'remember_token',
    ];

    /**
     * Casts.
     */
    protected $casts = [
        'email_verified_at' => 'datetime',
    ];

    /* -----------------------------------------------------------------
     | JWT
     |-----------------------------------------------------------------*/
    public function getJWTIdentifier()
    {
        return $this->getKey();
    }

    public function getJWTCustomClaims(): array
    {
        return [
            'is_admin' => $this->is_admin,
        ];
    }

    /* -----------------------------------------------------------------
     | Notifications
     |-----------------------------------------------------------------*/
    public function sendEmailVerificationNotification(): void
    {
        $this->notify(new VerifyEmail());
    }

    public function sendPasswordResetNotification($token): void
    {
        $this->notify(new \App\Notifications\ResetPasswordNotification($token));
    }

    /* -----------------------------------------------------------------
     | Relations
     |-----------------------------------------------------------------*/
    public function sports()
    {
        return $this->belongsToMany(Sport::class, 'user_sports');
    }

    /* -----------------------------------------------------------------
     | Accessors
     |-----------------------------------------------------------------*/
    /**
     * Always return a full URL (or default placeholder) for the avatar.
     */
    public function getAvatarUrlAttribute($value): string
    {
        return $value
            ? Storage::url($value)            // e.g. /storage/avatars/abc.png
            : asset('/images/avatar-default.jpg');
    }
}
