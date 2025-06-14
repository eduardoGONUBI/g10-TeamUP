<?php

namespace App\Notifications;

use Illuminate\Notifications\Notification;
use Illuminate\Support\Facades\Config;
use Illuminate\Notifications\Messages\MailMessage;

class ResetPasswordNotification extends Notification
{
    /**
     * The password reset token.
     *
     * @var string
     */
    public $token;

    /**
     * The callback that should be used to create the reset password URL.
     *
     * @var callable|null
     */
    public static $createUrlCallback;

    /**
     * Create a notification instance.
     */
    public function __construct(string $token)
    {
        $this->token = $token;
    }

    /**
     * Get the notification's delivery channels.
     */
    public function via($notifiable): array
    {
        return ['mail'];
    }

    /**
     * Build the mail representation of the notification.
     */
    public function toMail($notifiable): MailMessage
    {
        // URL DO frontend
        $frontend = 'http://localhost:5173';

        // constroi o link de reset
        $email = urlencode($notifiable->getEmailForPasswordReset());
        $link  = "{$frontend}/reset-password?token={$this->token}&email={$email}";

        return (new MailMessage)
            ->subject('Reset Password Notification')
            ->line('You are receiving this email because we received a password reset request for your account.')
            // envia o link
            ->action('Reset Password', $link)
            ->line('This password reset link will expire in '
                . Config::get('auth.passwords.'.config('auth.defaults.passwords').'.expire')
                . ' minutes.')
            ->line('If you did not request a password reset, no further action is required.');
    }
}
