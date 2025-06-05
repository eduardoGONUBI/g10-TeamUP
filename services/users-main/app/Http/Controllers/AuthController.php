<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Models\User;
use PHPOpenSourceSaver\JWTAuth\Facades\JWTAuth;
use PhpAmqpLib\Connection\AMQPStreamConnection;
use PhpAmqpLib\Message\AMQPMessage;
use Illuminate\Support\Facades\Storage;
use Illuminate\Validation\Rules\Password as PasswordRule;
use Illuminate\Support\Facades\Hash;

class AuthController extends Controller
{
    // Register a new user
    public function register(Request $request)
    {
        $validatedData = $request->validate([
            'name' => 'required|string|unique:users|regex:/^\w+$/|max:255',
            'email' => 'required|string|email|max:255|unique:users',
            'password' => 'required|string|min:6|confirmed',
            'location' => 'nullable|string|max:255',
            'sports' => 'array',
        ]);

        $user = User::create([
            'name' => $validatedData['name'],
            'email' => $validatedData['email'],
            'password' => bcrypt($validatedData['password']),
            'sport' => $validatedData['sport'] ?? null,
            'location' => $validatedData['location'] ?? null,
        ]);

        if ($request->has('sports')) {
            $user->sports()->sync($request->sports);
        }

        // Send email verification notification
        $user->sendEmailVerificationNotification();

        return response()->json([
            'message' => 'User registered successfully. Please check your email for verification instructions.',
        ], 201);
    }

    // User login
    public function login(Request $request)
    {
        $credentials = $request->only('email', 'password');

        // Attempt to authenticate the user
        if (!auth()->attempt($credentials)) {
            return response()->json(['error' => 'Unauthorized'], 401);
        }

        // Get the authenticated user
        $user = auth()->user();

        // Check if the user's email is verified
        if (!$user->hasVerifiedEmail()) {
            return response()->json(['error' => 'Email not verified'], 403);
        }

        // Include the user's name in the token claims
        $customClaims = ['name' => $user->name];

        // Generate a token with custom claims
        $token = JWTAuth::claims($customClaims)->fromUser($user);

        // Return the token including user's name and id
        return $this->respondWithToken($token, [
            'id' => $user->id,
            'name' => $user->name,
            'location' => $user->location,
        ]);
    }

    // Helper function to respond with token
    protected function respondWithToken($token, array $additionalPayload = [])
    {
        return response()->json([
            'access_token' => $token,
            'token_type' => 'bearer',
            'expires_in' => JWTAuth::factory()->getTTL() * 60,
            'payload' => $additionalPayload,
        ]);
    }

    public function updateAvatar(Request $request)
    {
        $request->validate([
            'avatar' => 'required|image|mimes:jpg,jpeg,png,gif,webp|max:2048',
        ]);

        $user = JWTAuth::user();       // same guard you already use

        // kill the old file (if any) before writing a new one
        if ($user->avatar && Storage::disk('public')->exists($user->avatar)) {
            Storage::disk('public')->delete($user->avatar);
        }

        // store() returns "avatars/xxxx.ext"
        $path = $request->file('avatar')->store('avatars', 'public');
        $user->avatar_url = $path;
        $user->save();

        return response()->json([
            'message' => 'Avatar updated ✨',
            'avatar_url' => $user->avatar_url,  // accessor above
        ]);
    }

    /**
     * GET /api/avatar/{id}
     * Sends the raw image file (good for native <img src="…"> tags).
     */
    public function getAvatar($id)
    {
        $user = User::findOrFail($id);

        // Grab the exact value from the DB (e.g. "avatars/8Rv3GqTGzqEgws9LUzF4BUB3iHLhZinNHSabg9we.jpg")
        $rawPath = $user->getRawOriginal('avatar_url');

        // If it’s empty or missing on disk, we abort with a 404
        if (!$rawPath || !Storage::disk('public')->exists($rawPath)) {
            abort(404, 'Avatar not found');
        }

        // Stream the file from storage/app/public/avatars/…
        return response()->file(
            Storage::disk('public')->path($rawPath)
        );
    }

    // Get the authenticated user
    public function me()
    {
        $user = JWTAuth::user()->load('sports');
        return response()->json($user);
    }

    public function logout()
    {
        try {
            $token = JWTAuth::getToken();
            JWTAuth::invalidate($token);

            // Optionally store the invalidated token in Redis for additional validation
            $redis = app('redis');
            $redis->setex('blacklist:' . $token, config('jwt.ttl') * 60, true);

            // Publish the blacklisted token to RabbitMQ
            $this->publishToRabbitMQ($token);

            return response()->json(['message' => 'Successfully logged out']);
        } catch (\Exception $e) {
            return response()->json(['error' => 'Something went wrong'], 500);
        }
    }

    // Helper function to publish to RabbitMQ and initialize Redis state
    protected function publishToRabbitMQ($token)
    {
        try {
            // Create a connection to RabbitMQ
            $connection = new AMQPStreamConnection(
                'rabbitmq', // RabbitMQ host
                5672,       // RabbitMQ port
                'guest',    // RabbitMQ user
                'guest'     // RabbitMQ password
            );
            $channel = $connection->channel();

            // Declare a fan-out exchange
            $exchange = 'fanout_exchange';
            $channel->exchange_declare($exchange, 'fanout', false, true, false);

            // Create a new message with the raw token as payload
            $msg = new AMQPMessage($token);

            // Publish the message to the exchange
            $channel->basic_publish($msg, $exchange);

            // Store the token in Redis with a counter for the number of consumers
            $redis = app('redis');
            $redis->setex("blacklisted:{$token}", config('jwt.ttl') * 60, 3); // 3 services

            // Close the channel and connection
            $channel->close();
            $connection->close();

            logger()->info("Published and initialized token: {$token}");
        } catch (\Exception $e) {
            // Log the error
            logger()->error("Failed to publish message to RabbitMQ: " . $e->getMessage());
        }
    }


    // Delete user account
    public function delete()
    {
        $user = JWTAuth::user();

        // Check if the user is an admin
        if ($user->is_admin) {
            return response()->json([
                'message' => 'Cannot delete admin.'
            ], 403); // 403 Forbidden
        }

        JWTAuth::invalidate(JWTAuth::getToken());
        $user->delete();

        return response()->json(['message' => 'User deleted successfully']);
    }

    // Update user details
    public function update(Request $request)
    {
        // Validate the incoming request
        $validatedData = $request->validate([
            'name' => [
                'sometimes',
                'required',
                'string',
                'unique:users,name,' . auth()->id(),
                'regex:/^\w+$/',
                'max:255'
            ],
            'email' => [
                'sometimes',
                'required',
                'string',
                'email',
                'max:255',
                'unique:users,email,' . auth()->id()
            ],
            'location' => 'sometimes|nullable|string|max:255',
            'sports' => 'sometimes|array',
            'sports.*' => 'exists:sports,id', // Validate that each sport ID exists
            'latitude' => 'sometimes|nullable|numeric|between:-90,90',
            'longitude' => 'sometimes|nullable|numeric|between:-180,180',
        ]);

        // Get the authenticated user
        $user = auth()->user();

        // Update name and email if they are provided
        if (isset($validatedData['name'])) {
            $user->name = $validatedData['name'];
        }
        if (isset($validatedData['email'])) {
            $user->email = $validatedData['email'];
        }
        if (isset($validatedData['location'])) {
            $user->location = $validatedData['location'];
        }

        // Sync sports if provided
        if (isset($validatedData['sports'])) {
            $user->sports()->sync($validatedData['sports']);
        }

        if (array_key_exists('latitude', $validatedData))
            $user->latitude = $validatedData['latitude'];
        if (array_key_exists('longitude', $validatedData))
            $user->longitude = $validatedData['longitude'];

        // Save the changes
        $user->save();

        // Return the updated user data
        return response()->json([
            'message' => 'User updated successfully',
            'user' => $user->load('sports'), // Include sports in the response
        ], 200);
    }


    /**  Alterar password  */
    public function changePassword(Request $request)
    {
        $request->validate([
            'current_password' => ['required'],
            'new_password' => ['required', 'confirmed', PasswordRule::defaults()],
        ]);

        $user = JWTAuth::user();                       // usa o mesmo guard JWT

        if (!Hash::check($request->current_password, $user->password)) {
            return response()->json(['message' => 'Password actual incorreta'], 422);
        }

        $user->password = Hash::make($request->new_password);
        $user->save();

        // ⚠ NÃO existe apagador de sessões em ambiente JWT
        // auth()->logoutOtherDevices($request->new_password);

        return response()->json(['message' => 'Password alterada com sucesso'], 200);
    }

    /**  Alterar e-mail  */
    public function changeEmail(Request $request)
    {
        $request->validate([
            'new_email' => 'required|email|unique:users,email',
            'password' => ['required'],
        ]);

        $user = JWTAuth::user();

        if (!Hash::check($request->password, $user->password)) {
            return response()->json(['message' => 'Password incorreta'], 422);
        }

        $user->email = $request->new_email;
        $user->email_verified_at = null;
        $user->save();

        $user->sendEmailVerificationNotification();

        return response()->json([
            'message' => 'E-mail alterado. Verifique a nova caixa de correio.',
        ], 200);
    }


}