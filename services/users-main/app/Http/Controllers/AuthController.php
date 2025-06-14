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
    /// registar um user
    public function register(Request $request)
    {
        $validatedData = $request->validate([
            'name' => 'required|string|unique:users|regex:/^\w+$/|max:255',    // nome unico apenas letras e numeros
            'email' => 'required|string|email|max:255|unique:users',       // email valido e unico
            'password' => 'required|string|min:6|confirmed',         // password min 6
            'location' => 'nullable|string|max:255',                 //   location
            'sports' => 'array',                   // array de ids
        ]);

        $user = User::create([         // cria na base de dados
            'name' => $validatedData['name'],
            'email' => $validatedData['email'],
            'password' => bcrypt($validatedData['password']),
            'sport' => $validatedData['sport'] ?? null,
            'location' => $validatedData['location'] ?? null,
        ]);

        if ($request->has('sports')) {
            $user->sports()->sync($request->sports);   // se registar com uma lista de desportos faz a relaçao many to many
        }


        $user->sendEmailVerificationNotification();          // manda email de confirmaçao

        return response()->json([
            'message' => 'User registered successfully. Please check your email for verification instructions.',        //resposta JSON
        ], 201);
    }

    // User login e gera token
    public function login(Request $request)
    {
        $credentials = $request->only('email', 'password');         // credencias do payload


        if (!auth()->attempt($credentials)) {                          // autentica o user
            return response()->json(['error' => 'Unauthorized'], 401);
        }


        $user = auth()->user();                // obtem o user autenticado


        if (!$user->hasVerifiedEmail()) {                          // verifica se tem o email verificado
            return response()->json(['error' => 'Email not verified'], 403);
        }


        $customClaims = ['name' => $user->name];   // mete o nome do user no token


        $token = JWTAuth::claims($customClaims)->fromUser($user);      // gera o token


        return $this->respondWithToken($token, [      // devolve o token com id nome e localizaçao
            'id' => $user->id,
            'name' => $user->name,
            'location' => $user->location,
        ]);
    }

    // Helper formatar a resposto do token
    protected function respondWithToken($token, array $additionalPayload = [])
    {
        return response()->json([
            'access_token' => $token,
            'token_type' => 'bearer',
            'expires_in' => JWTAuth::factory()->getTTL() * 60,
            'payload' => $additionalPayload,
        ]);
    }

    // atualia foto de perfil
    public function updateAvatar(Request $request)
    {
        //valida a imagem pelo tipo e tamanho max
        $request->validate([
            'avatar' => 'required|image|mimes:jpg,jpeg,png,gif,webp|max:2048',
        ]);

        $user = JWTAuth::user();       // obtem o utilizador autenticado

        // se ja existir um ficheiro elimina o
        if ($user->avatar && Storage::disk('public')->exists($user->avatar)) {
            Storage::disk('public')->delete($user->avatar);
        }

        // guarda o novo ficheiro em storage/app/public/avatar
        $path = $request->file('avatar')->store('avatars', 'public');
        // atualiza o campo avatar do utilizador
        $user->avatar_url = $path;
        $user->save();

        return response()->json([    // sucesso
            'message' => 'Avatar updated ✨',
            'avatar_url' => $user->avatar_url,
        ]);
    }

    // vai buscar a foto de perfil de um utilizador
    public function getAvatar($id)
    {
        $user = User::findOrFail($id);   // procura o utilizador por ID


        $rawPath = $user->getRawOriginal('avatar_url');  // vai buscar o caminho do ficheiro

        // se tiver vazio
        if (!$rawPath || !Storage::disk('public')->exists($rawPath)) {
            abort(404, 'Avatar not found');
        }

        // devolve a imagem com ficheiro
        return response()->file(
            Storage::disk('public')->path($rawPath)
        );
    }

    // devolve dados do utilizador autenticado
    public function me()
    {
        $user = JWTAuth::user()->load('sports');     // carrega a relaçao de desportos
        return response()->json($user);     //retorna os dados em JSON
    }

    //logout
    public function logout()
    {
        try {
            $token = JWTAuth::getToken();     //obtem token
            JWTAuth::invalidate($token);     // invalida token

            // guarda o token invalido no redis
            $redis = app('redis');
            $redis->setex('blacklist:' . $token, config('jwt.ttl') * 60, true);

            // public o token invalida no rabbit para todos microserviços saberem que esta invalido
            $this->publishToRabbitMQ($token);

            return response()->json(['message' => 'Successfully logged out']);
        } catch (\Exception $e) {
            return response()->json(['error' => 'Something went wrong'], 500);
        }
    }

    // Helper  para mandar mensagem no broker ( neste caso so para enviar o token invalido)
    protected function publishToRabbitMQ($token)
    {
        try {
            // Cria a conexao no RabbitMQ
            $connection = new AMQPStreamConnection(
                'rabbitmq', // RabbitMQ host
                5672,       // RabbitMQ port
                'guest',    // RabbitMQ user
                'guest'     // RabbitMQ password
            );
            $channel = $connection->channel();

            // fan-out exchange   (distribui a todos os consumidores ligados)
            $exchange = 'fanout_exchange';
            $channel->exchange_declare($exchange, 'fanout', false, true, false);

            // Cria a mensagem com o token
            $msg = new AMQPMessage($token);

            // publica a mensagem
            $channel->basic_publish($msg, $exchange);

            // regista no redis o token com quandos microserviços devem consumi lo
            $redis = app('redis');
            $redis->setex("blacklisted:{$token}", config('jwt.ttl') * 60, 3); // 3 serviços

            // fecha canal e conexao
            $channel->close();
            $connection->close();

            logger()->info("Published and initialized token: {$token}");  //sucesso
        } catch (\Exception $e) {
            // Log the error
            logger()->error("Failed to publish message to RabbitMQ: " . $e->getMessage());
        }
    }


    // Apaga a conta do utilizador autenticado
    public function delete()
    {
        $user = JWTAuth::user();   // obtem o utilizador autenticado

        // verifica se é admion
        if ($user->is_admin) {
            return response()->json([
                'message' => 'Cannot delete admin.'
            ], 403); // 403 Forbidden
        }

        JWTAuth::invalidate(JWTAuth::getToken());    // invalida o token
        $user->delete();     //remove da base de dados

        return response()->json(['message' => 'User deleted successfully']);
    }

    // atualiza dados do utilizador autenticado
    public function update(Request $request)
    {
        // valida os campos recebidos (tudo opcional)
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
            'sports.*' => 'exists:sports,id',
            'latitude' => 'sometimes|nullable|numeric|between:-90,90',
            'longitude' => 'sometimes|nullable|numeric|between:-180,180',
        ]);

        // obtem o utilizador autenticado
        $user = auth()->user();

        // atualiza campos se vierem no payload
        if (isset($validatedData['name'])) {
            $user->name = $validatedData['name'];
        }
        if (isset($validatedData['email'])) {
            $user->email = $validatedData['email'];
        }
        if (isset($validatedData['location'])) {
            $user->location = $validatedData['location'];
        }

        // sincroniza os desportos
        if (isset($validatedData['sports'])) {
            $user->sports()->sync($validatedData['sports']);
        }

        // atualiza as coordenadas se fornecidas
        if (array_key_exists('latitude', $validatedData))
            $user->latitude = $validatedData['latitude'];
        if (array_key_exists('longitude', $validatedData))
            $user->longitude = $validatedData['longitude'];

        // guarda na base de dados
        $user->save();

        // devolve json suceeso e dados
        return response()->json([
            'message' => 'User updated successfully',
            'user' => $user->load('sports'),
        ], 200);
    }


    //  Alterar password  
    public function changePassword(Request $request)
    {
        // valida a palavra passe 
        $request->validate([
            'current_password' => ['required'],
            'new_password' => ['required', 'confirmed', PasswordRule::defaults()],
        ]);

        $user = JWTAuth::user();                       // obtem user autenticado

        // verifica se a pass atual esta correta
        if (!Hash::check($request->current_password, $user->password)) {
            return response()->json(['message' => 'Password actual incorreta'], 422);
        }

        // grava a nova palavra passe na base de dados
        $user->password = Hash::make($request->new_password);
        $user->save();


        // sucesso em json
        return response()->json(['message' => 'Password alterada com sucesso'], 200);
    }

    // Alterar e-mail  
    public function changeEmail(Request $request)
    {
        $request->validate([      //valida novo email e password
            'new_email' => 'required|email|unique:users,email',
            'password' => ['required'],
        ]);

        $user = JWTAuth::user();   // obtem user autenticado

        // verifica se password esta correta
        if (!Hash::check($request->password, $user->password)) {
            return response()->json(['message' => 'Password incorreta'], 422);
        }

        // atualiza email e marca como por verficiar
        $user->email = $request->new_email;
        $user->email_verified_at = null;
        $user->save();

        $user->sendEmailVerificationNotification();  // manda email de confirmaçao

        return response()->json([
            'message' => 'E-mail alterado. Verifique a nova caixa de correio.',   //sucesso
        ], 200);
    }


}