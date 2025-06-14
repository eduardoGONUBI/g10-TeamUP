<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Http;
use Illuminate\Support\Facades\Log;
use App\Models\Event;
use App\Models\Participant;
use Illuminate\Support\Facades\Cache;
use PHPOpenSourceSaver\JWTAuth\Facades\JWTAuth;
use PhpAmqpLib\Connection\AMQPStreamConnection;
use PhpAmqpLib\Message\AMQPMessage;
use Illuminate\Support\Facades\DB;
use Carbon\Carbon;
use App\Services\WeatherService;

class EventController extends Controller
{

    // serviço de metereologia
    private $weatherService;

    public function __construct(WeatherService $weatherService)
    {
        $this->weatherService = $weatherService;
    }

    //───────────────────────────────────────────── TOKENS ────────────────────────────────────────────────────────────────────────────────────
    //verifica se um token esta blacklisted
    private function isTokenBlacklisted($token)
    {
        $cacheKey = "blacklisted:{$token}";

        return Cache::has($cacheKey);
    }

    // extrai o token do pedido
    private function getTokenFromRequest(Request $request)
    {
        $token = $request->header('Authorization');

        if (!$token) {
            throw new \Exception('Token is required.');
        }

        // Remove o prefixo "Bearer " caso exista
        if (str_starts_with($token, 'Bearer ')) {
            $token = substr($token, 7);
        }

        return $token;  // devolve token limpo
    }

    // valida o token
    private function validateToken(Request $request)
    {
        $token = $this->getTokenFromRequest($request);

        if ($this->isTokenBlacklisted($token)) {
            throw new \Exception('Unauthorized: Token is blacklisted.');
        }

        return $token;  // retorna se for valido
    }

    // extrai o nome pelo token
    private function getUserNameFromToken($token)
    {
        $payload = JWTAuth::setToken($token)->getPayload();
        return $payload->get('name') ?? null;
    }


    //───────────────────────────────────────────── RABBIT ────────────────────────────────────────────────────────────────────────────────────
    // publica uma mensagem numa fila de rabbitmq especifica
    private function publishToRabbitMQ($queueName, $messageBody)
    {
        try {  // estabelece ligaçao
            $connection = new AMQPStreamConnection('rabbitmq', 5672, 'guest', 'guest');
            $channel = $connection->channel();

            // declara a fila
            $channel->queue_declare($queueName, false, true, false, false);

            // cria a mensagem como persistente
            $msg = new AMQPMessage($messageBody, [
                'delivery_mode' => AMQPMessage::DELIVERY_MODE_PERSISTENT
            ]);

            // publica a mensagem
            $channel->basic_publish($msg, '', $queueName);


            //fecha canal e ligaçao
            $channel->close();
            $connection->close();
        } catch (\Exception $e) {

        }
    }

    //───────────────────────────────────────────── CRIAR EVENTO ────────────────────────────────────────────────────────────────────────────────────
    public function store(Request $request)
    {
        try {
            // valida e extrai token
            $token = $this->validateToken($request);

            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            //valida os campos inseridos
            $validatedData = $request->validate([
                'name' => 'required|string',
                'sport_id' => 'required|exists:sports,id',
                'starts_at' => 'required|date_format:Y-m-d H:i:s',
                'place' => 'required|string',
                'max_participants' => 'required|integer|min:2',
            ]);

            // -----------------GOOGLE MAPS API -------------------------------
            // geocode localizaçao
            $geoKey = config('services.google_maps.key')
                ?? env('GOOGLE_MAPS_GEOCODE_API_KEY');
            if (!$geoKey) {
                throw new \Exception('Missing Google Geocoding API key');
            }
            // obtem coordenadas da localizaçao
            $geoRes = Http::get('https://maps.googleapis.com/maps/api/geocode/json', [
                'address' => $validatedData['place'],
                'key' => $geoKey,
            ])->json();

            if (($geoRes['status'] ?? '') === 'ZERO_RESULTS') {
                // User typed something Google doesn't recognize
                return response()->json([
                    'error' => 'Address not found. Please enter a valid location.'
                ], 422);
            }

            if (
                ($geoRes['status'] ?? '') !== 'OK' ||
                empty($geoRes['results'][0]['geometry']['location'])
            ) {
                $status = $geoRes['status'] ?? 'UNKNOWN';
                return response()->json([
                    'error' => "Error while searching for the address: {$status}. Please try again."
                ], 422);
            }

            // extrai coordenadas
            $loc = $geoRes['results'][0]['geometry']['location'];
            $lat = $loc['lat'];
            $lng = $loc['lng'];

            // --------------WEATHER API------------------------------
            $eventDate = Carbon::parse($validatedData['starts_at'])->format('Y-m-d');   // dia para ver
            try { // vai buscar o weather
                $weatherData = $this->weatherService->getForecastForDate($lat, $lng, $eventDate);
                if (!$weatherData || empty($weatherData)) {
                    $weatherData = null;
                }
            } catch (\Throwable $ex) {
                $weatherData = null; // failure fica null
            }

            // --------------Cria o evento------------------------------
            $event = Event::create([      // insere na BD
                'name' => $validatedData['name'],
                'sport_id' => $validatedData['sport_id'],
                'starts_at' => $validatedData['starts_at'],
                'place' => $validatedData['place'],
                'user_id' => $userId,
                'user_name' => $userName,
                'status' => 'in progress',
                'max_participants' => $validatedData['max_participants'],
                'latitude' => $lat,
                'longitude' => $lng,
                'weather' => $weatherData ? json_encode($weatherData) : null,
            ]);

            // mete o criador como participante
            Participant::create([
                'event_id' => $event->id,
                'user_id' => $userId,
                'user_name' => $userName,
            ]);

            //  // --------------notifica microserviços via rabbit-----------------------------
            $msg = [
                'event_id' => $event->id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'User joined the event',
            ];
            $this->publishToRabbitMQ('event_joined', json_encode($msg));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($msg));
            $this->publishToRabbitMQ('lolchat_event_join-leave', json_encode($msg));
            $this->publishToRabbitMQ('ach_event_join-leave', json_encode($msg));
            $this->publishToRabbitMQ('noti_event_join-leave', json_encode($msg));

            return response()->json([
                'message' => 'Evento criado com sucesso!',    // sucess
                'event' => $event,
            ], 201);

        } catch (\Exception $e) {

            return response()->json(['error' => $e->getMessage()], 400);
        }
    }



    // transforma uma morada em coordenadas
    private function geocodePlace(string $place): array
    {
        $resp = Http::get(
            'https://maps.googleapis.com/maps/api/geocode/json',
            ['address' => $place, 'key' => env('GOOGLE_GEOCODE_KEY')]
        );

        if (!$resp->ok() || empty($resp['results'][0]['geometry']['location'])) {
            return [null, null];
        }

        $loc = $resp['results'][0]['geometry']['location'];
        return [$loc['lat'], $loc['lng']];
    }

    //───────────────────────────────────────────── MOSTRAR EVENTO ESPECIFICO ────────────────────────────────────────────────────────────────────────────────────
    // mostra dados de um evento
    public function show(Request $request, $id)
    {
        // valida o token
        $token = $this->validateToken($request);

        // pega no evento
        $event = Event::findOrFail($id);

        // pega no weather
        $raw = is_array($event->weather)
            ? $event->weather
            : json_decode($event->weather, true) ?? [];

        // normaliza os dados do weather
        $weather = [
            'temp' => $raw['temp'] ?? null,
            'high_temp' => $raw['high_temp'] ?? null,
            'low_temp' => $raw['low_temp'] ?? null,
            'description' => $raw['weather']['description'] ?? null,
        ];

        // busca os participantes
        $participants = DB::table('event_user')
            ->where('event_id', $event->id)
            ->whereNull('deleted_at')
            ->get(['user_id', 'user_name', 'rating'])
            ->map(function ($p) {
                return [
                    'id' => $p->user_id,
                    'name' => $p->user_name,
                    'rating' => $p->rating,
                    'avatar_url' => null,
                ];
            });

        // devolve como JSON
        return response()->json([
            'id' => $event->id,
            'name' => $event->name,
            'sport' => $event->sport->name ?? null,
            'starts_at' => $event->starts_at,
            'place' => $event->place,
            'status' => $event->status,
            'max_participants' => $event->max_participants,
            'latitude' => $event->latitude,
            'longitude' => $event->longitude,
            'user_id' => $event->user_id,
            'creator' => [
                'id' => $event->user_id,
                'name' => $event->user_name,
            ],
            'weather' => $weather,
            'participants' => $participants,
        ], 200);
    }

    //───────────────────────────────────────────── LISTA EVENTOS CRIADOS ────────────────────────────────────────────────────────────────────────────────────

    // formata eventos para a paginaçao
    private function formatEvent(Event $event): array
    {


        // -----------metereologia --------------------------
        $raw = is_array($event->weather)
            ? $event->weather
            : json_decode($event->weather, true) ?? [];

        $weather = [
            'app_max_temp' => $raw['app_max_temp'] ?? null,
            'app_min_temp' => $raw['app_min_temp'] ?? null,
            'temp' => $raw['temp'] ?? null,
            'high_temp' => $raw['high_temp'] ?? null,
            'low_temp' => $raw['low_temp'] ?? null,
            'description' => $raw['weather']['description'] ?? null,
        ];
        // ------- participantes -----------------
        $participants = DB::table('event_user')
            ->where('event_id', $event->id)
            ->whereNull('deleted_at')
            ->get(['user_id', 'user_name', 'rating'])
            ->map(fn($p) => [
                'id' => $p->user_id,
                'name' => $p->user_name,
                'rating' => $p->rating,
            ]);
        //---------retorna evento formatado com array ----------------------
        return [
            'id' => $event->id,
            'name' => $event->name,
            'sport' => $event->sport->name ?? null,
            'starts_at' => $event->starts_at,
            'place' => $event->place,
            'status' => $event->status,
            'max_participants' => $event->max_participants,
            'latitude' => $event->latitude,
            'longitude' => $event->longitude,
            'creator' => [
                'id' => $event->user_id,
                'name' => $event->user_name,
            ],
            'weather' => $weather,
            'participants' => $participants,
        ];
    }


    // Listar eventos criados pelo utilizador autenticado com paginação.
    public function index(Request $request)
    {
        // valida token
        $token = $this->validateToken($request);
        $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

        // define o numero de eventos por pagina (default :7)
        $perPage = min(max((int) $request->query('per_page', 7), 1), 100);

        // inicia a query dos eventos
        $paginator = Event::with('sport')
            ->where('user_id', $userId)
            ->orderByDesc('created_at')
            ->paginate($perPage);

        // obtem os eventos da pagina atual  e chama a funçao formatevent 
        $paginator->getCollection()->transform(function ($event) {
            return $this->formatEvent($event);
        });

        return response()->json([   // retorna JSON com dados paginados
            'data' => $paginator->items(),
            'meta' => [
                'current_page' => $paginator->currentPage(),
                'last_page' => $paginator->lastPage(),
                'per_page' => $paginator->perPage(),
                'total' => $paginator->total(),
            ],
        ]);
    }
    //───────────────────────────────────────────── APAGAR EVENTO ────────────────────────────────────────────────────────────────────────────────────
    public function destroy(Request $request, $id)
    {
        try {
            $token = $this->validateToken($request); // valida token


            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');   // obtem id do user autenticado

            // encontra o evento
            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // verifica se utilizador é o criador do evento
            if ((int) $event->user_id !== (int) $userId) {
                return response()->json(['error' => 'Unauthorized'], 403);
            }

            // dados para publicar no rabbit
            $eventDataForChatDeletion = ['event_id' => $event->id];

            // apaga evento da BD
            $event->delete();

            // publica a mensagem no rabbitmq 
            $this->publishToRabbitMQ('event_deleted', json_encode($eventDataForChatDeletion));

            return response()->json(['message' => 'Event deleted successfully'], 200);  // sucess
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    //───────────────────────────────────────────── UPDATE EVENTO ────────────────────────────────────────────────────────────────────────────────────
    public function update(Request $request, $id)
    {
        try {
            $token = $this->validateToken($request);   //valida token
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');   // extrai id do user

            $event = Event::find($id);    // encontra evento

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            if ((int) $event->user_id !== (int) $userId) {    // verifica se e criador 
                return response()->json(['error' => 'Unauthorized'], 403);
            }

            $validatedData = $request->validate([    // valida campos que foram enviados
                'name' => 'string|nullable',
                'sport_id' => 'exists:sports,id|nullable',
                'date' => 'date|nullable',
                'place' => 'string|nullable',
                'max_participants' => 'integer|min:2|nullable',
                'status' => 'in:in progress,concluded|nullable',
            ]);

            $event->update($validatedData);    // atualiza na BD

            // obtem os participantes do evento 
            $participants = Participant::where('event_id', $id)
                ->distinct()
                ->get(['user_id', 'user_name'])
                ->toArray();

            // Prepare payload
            $notificationMessage = [
                'type' => 'new_message',
                'event_id' => $id,
                'event_name' => $event->name ?? "Evento $id",
                'user_id' => $userId,
                'user_name' => $this->getUserNameFromToken($token),
                'message' => 'Event was Updated',
                'timestamp' => now()->toISOString(),
                'participants' => $participants,
            ];

            //publica no rabbit na fila notification
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['message' => 'Event updated successfully', 'event' => $event], 200);  // sucesso
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }



    //───────────────────────────────────────────── JOIN EVENT ────────────────────────────────────────────────────────────────────────────────────
    public function join(Request $request, $id)
    {
        try { //  // valida token e extrai nome e id
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            $event = Event::find($id);  // encontra evento

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // verifica se ja acabou o evento
            if ($event->status === 'concluded') {
                return response()->json(['error' => 'This event has concluded. You can no longer join.'], 400);
            }
            // verifica se é o criador do evento
            if ($event->user_id === $userId) {
                return response()->json(['error' => 'You cannot join your own event'], 400);
            }
            // verifica limite de participantes
            $currentParticipantCount = Participant::where('event_id', $id)->count();
            if ($currentParticipantCount >= $event->max_participants) {
                return response()->json(['error' => 'Event is full'], 400);
            }
            // verifica se ja esta no evento
            if (Participant::where('event_id', $id)->where('user_id', $userId)->exists()) {
                return response()->json(['error' => 'You have already joined this event'], 400);
            }

            // cria o participante
            Participant::create([
                'event_id' => $id,
                'user_id' => $userId,
                'user_name' => $userName,
            ]);


            // obtem lista dos participantes atual
            $participants = Participant::where('event_id', $id)->get(['user_id', 'user_name'])->toArray();

            // prepara payload
            $notificationMessage = [
                'type' => 'new_message',
                'event_id' => $id,
                'event_name' => "Evento $id",
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'A User has joined the event',
                'timestamp' => now()->toISOString(),
                'participants' => $participants, // Already an array
            ];

            //   //publica no rabbit na fila notification
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));


            // Prepara payload para o chat
            $messageDataForChat = [
                'event_id' => $id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'User joined the event'
            ];

            // P publica no rabbit em varias filas
            $this->publishToRabbitMQ('event_joined', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('lolchat_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('ach_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('noti_event_join-leave', json_encode($messageDataForChat));



            return response()->json(['message' => 'You have successfully joined the event'], 200);  // sucesso
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    //───────────────────────────────────────────── LISTA PARTICIPANTES ────────────────────────────────────────────────────────────────────────────────────
    public function participants(Request $request, $id)
    {
        try {
            //valida token e extrai id
            $token = $this->validateToken($request);
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            // encontra o evento
            $event = Event::find($id);
            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }
            //verifica se e criador ou participante
            $isAuthorized =
                ($event->user_id == $userId) ||
                Participant::where('event_id', $id)
                    ->where('user_id', $userId)
                    ->exists();

            if (!$isAuthorized) {
                return response()->json(
                    ['error' => 'Unauthorized: You do not have access to view this event'],
                    403
                );
            }

            // busca participantes do evento
            $participants = DB::table('event_user')
                ->where('event_id', $id)
                ->whereNull('deleted_at')
                ->get(['user_id', 'user_name', 'rating'])
                ->map(function ($p) {
                    return [
                        'id' => $p->user_id,
                        'name' => $p->user_name,
                        'rating' => $p->rating,
                        'avatar_url' => null,
                    ];
                });

            //retorna json com os dados de cada participante
            return response()->json([
                'event_id' => $event->id,
                'event_name' => $event->name,
                'creator' => [
                    'id' => $event->user_id,
                    'name' => $event->user_name,
                ],
                'participants' => $participants,
            ], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 500);
        }
    }


    //───────────────────────────────────────────── LISTA USER EVENTOS────────────────────────────────────────────────────────────────────────────────────
// lista eventos criados ou participando do user autenticado com paginaçao
    public function userEvents(Request $request)
    {
        try {
            // valida token e extrai id
            $token = $this->validateToken($request);
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            // define o numero de eventos por pagina (default :15)
            $perPage = min(max((int) $request->query('per_page', 15), 1), 100);

            // inicia a query dos eventos
            $paginator = Event::with('sport')
                ->where('user_id', $userId)
                ->orWhereHas('participants', function ($q) use ($userId) {
                    $q->where('user_id', $userId);
                })
                ->orderByDesc('created_at')
                ->paginate($perPage);

            // obtem os eventos da pagina atual  e formata para paginas 
            $paginator->getCollection()->transform(function ($event) {
                $rawWeather = is_array($event->weather)
                    ? $event->weather
                    : json_decode($event->weather, true) ?? [];

                $weather = [  // formata dados de meterologia
                    'app_max_temp' => $rawWeather['app_max_temp'] ?? 'N/A',
                    'app_min_temp' => $rawWeather['app_min_temp'] ?? 'N/A',
                    'temp' => $rawWeather['temp'] ?? 'N/A',
                    'high_temp' => $rawWeather['high_temp'] ?? 'N/A',
                    'low_temp' => $rawWeather['low_temp'] ?? 'N/A',
                    'description' => $rawWeather['weather']['description'] ?? 'N/A',
                ];

                $participants = DB::table('event_user')  // busca e formata participantes
                    ->where('event_id', $event->id)
                    ->whereNull('deleted_at')
                    ->get(['user_id', 'user_name', 'rating'])
                    ->map(function ($p) {
                        return [
                            'id' => $p->user_id,
                            'name' => $p->user_name,
                            'rating' => $p->rating,
                        ];
                    });

                return [         //  devolve evento formatado
                    'id' => $event->id,
                    'name' => $event->name,
                    'sport' => $event->sport->name ?? null,
                    'starts_at' => $event->starts_at,
                    'place' => $event->place,
                    'status' => $event->status,
                    'max_participants' => $event->max_participants,
                    'latitude' => $event->latitude,
                    'longitude' => $event->longitude,
                    'creator' => [
                        'id' => $event->user_id,
                        'name' => $event->user_name,
                    ],
                    'weather' => $weather,
                    'participants' => $participants,
                ];
            });


            return response()->json([   // resposta com dados de paginaçao
                'data' => $paginator->items(),
                'meta' => [
                    'current_page' => $paginator->currentPage(),
                    'per_page' => $paginator->perPage(),
                    'last_page' => $paginator->lastPage(),
                    'total' => $paginator->total(),
                ],
            ], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    //───────────────────────────────────────────── SEARCH EVENT ────────────────────────────────────────────────────────────────────────────────────
    public function search(Request $request)
    {
        try {
            //valida token
            $token = $this->validateToken($request);

            // define o numero de eventos por pagina (default :15)
            $perPage = min(max((int) $request->query('per_page', 15), 1), 100);

            //  query de pesquisa veririca que campos foram inseridos
            $query = Event::with('sport');

            if ($request->has('id')) {
                $query->where('id', $request->input('id'));
            }
            if ($request->has('name')) {
                $query->where('name', 'like', '%' . $request->input('name') . '%');
            }
            if ($request->has('date')) {
                $query->where('starts_at', 'like', $request->input('date') . '%');
            }
            if ($request->has('place')) {
                $query->where('place', 'like', '%' . $request->input('place') . '%');
            }

            // Paginaçao e ordenaça
            $paginator = $query
                ->orderByDesc('starts_at')
                ->paginate($perPage);

            // obtem os eventos da pagina atual  e formata para paginas 
            $paginator->getCollection()->transform(function ($event) {
                $rawWeather = is_array($event->weather)
                    ? $event->weather
                    : json_decode($event->weather, true) ?? [];

                $weather = [   //formatada dados de metereologia
                    'app_max_temp' => $rawWeather['app_max_temp'] ?? null,
                    'app_min_temp' => $rawWeather['app_min_temp'] ?? null,
                    'temp' => $rawWeather['temp'] ?? null,
                    'high_temp' => $rawWeather['high_temp'] ?? null,
                    'low_temp' => $rawWeather['low_temp'] ?? null,
                    'description' => $rawWeather['weather']['description'] ?? null,
                ];

                $participants = DB::table('event_user')  // busca participantes do evento
                    ->where('event_id', $event->id)
                    ->whereNull('deleted_at')
                    ->get(['user_id', 'user_name', 'rating'])
                    ->map(function ($p) {
                        return [
                            'id' => $p->user_id,
                            'name' => $p->user_name,
                            'rating' => $p->rating,
                        ];
                    });

                return [  // retorna o array formatado do evento
                    'id' => $event->id,
                    'name' => $event->name,
                    'sport' => $event->sport->name ?? null,
                    'starts_at' => $event->starts_at,
                    'place' => $event->place,
                    'status' => $event->status,
                    'max_participants' => $event->max_participants,
                    'latitude' => $event->latitude,
                    'longitude' => $event->longitude,
                    'creator' => [
                        'id' => $event->user_id,
                        'name' => $event->user_name,
                    ],
                    'weather' => $weather,
                    'participants' => $participants,
                ];
            });

            // resposta com dados de paginaçao
            return response()->json([
                'data' => $paginator->items(),
                'meta' => [
                    'current_page' => $paginator->currentPage(),
                    'per_page' => $paginator->perPage(),
                    'last_page' => $paginator->lastPage(),
                    'total' => $paginator->total(),
                ],
            ], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    //───────────────────────────────────────────── CONCLUIR EVENTO────────────────────────────────────────────────────────────────────────────────────
    public function concludeByCreator(Request $request, $id)
    {
        try {
            // valida token e extrai id e nome
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            // encontra evento
            $event = Event::find($id);
            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }
            if ((int) $event->user_id !== (int) $userId) {
                return response()->json(['error' => 'Unauthorized: only the creator can end the event'], 403);
            }
            if ($event->status === 'concluded') {
                return response()->json(['message' => 'Event is already concluded'], 200);
            }

            // por evento com o concluido na BD
            $wasConcluded = $event->status === 'concluded';
            $event->status = 'concluded';
            $event->save();

            //  depois de concluido publica a mensagem no rabbit
            if (!$wasConcluded && $event->status === 'concluded') {
                $lifecycleMessage = [
                    'event_id' => $event->id,
                    'event_name' => $event->name,
                    'user_id' => $userId,
                    'user_name' => $userName,
                    'message' => 'Event concluded by creator',
                ];
                // Para o micro-serviço de achievements e rating
                $this->publishToRabbitMQ('event_concluded', json_encode($lifecycleMessage));
                $this->publishToRabbitMQ('rating_event_concluded', json_encode($lifecycleMessage));
            }

            // Notificação chat fan-out
            $participants = Participant::where('event_id', $id)   // vai buscar os participantes
                ->get(['user_id', 'user_name'])
                ->toArray();

            $notification = [    // mensagem
                'type' => 'new_message',
                'event_id' => $event->id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'The event has ended',
                'timestamp' => now()->toISOString(),
                'participants' => $participants,
            ];
            $this->publishToRabbitMQ('notification', json_encode($notification)); // publica 

            return response()->json([
                'message' => 'Event concluded successfully',    //sucesso
                'event' => $event,
            ], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    //───────────────────────────────────────────── leave EVENTO────────────────────────────────────────────────────────────────────────────────────
    public function leave(Request $request, $id)
    {
        try {
            // valida token e retorna id e name
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            // procura o evento
            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // verifica se o user é participante
            $participant = Participant::where('event_id', $id)->where('user_id', $userId)->first();

            if (!$participant) {
                return response()->json(['error' => 'You are not a participant of this event'], 400);
            }

            // apagar a participaçao na BD
            $participant->delete();

            //  publica no rabbit 
            $messageDataForChat = [
                'event_id' => $id,
                'user_id' => $userId
            ];

            $this->publishToRabbitMQ('user_left_event', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('lolchat_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('ach_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('noti_event_join-leave', json_encode($messageDataForChat));

            // busca todos os participantes
            $remainingParticipants = Participant::where('event_id', $id)->get(['user_id', 'user_name'])->toArray();


            // prepara a mensagem
            $notificationMessage = [
                'type' => 'new_message',
                'event_id' => $id,
                'event_name' => "Evento $id",
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'A user has left the event',
                'timestamp' => now()->toISOString(),
                'participants' => $remainingParticipants, // Already an array
            ];

            // publica a mensagem na fila 'notification'
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['message' => 'You have successfully left the event'], 200);  // sucesso
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    //───────────────────────────────────────────── EXPULSAR PARTICIPANTE ────────────────────────────────────────────────────────────────────────────────────
    public function kickParticipant(Request $request, $event_id, $user_id)
    {
        try {
            // valida token e extrai id
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $currentUserId = $payload->get('sub');

            // procura evento
            $event = Event::find($event_id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // verifica se é o criador
            if ((int) $event->user_id !== (int) $currentUserId) {
                return response()->json(['error' => 'You are not the creator of this event'], 403);
            }

            // verifica se o que esta a ser expulso é um participante
            $participant = Participant::where('event_id', $event_id)
                ->where('user_id', $user_id)
                ->first();

            if (!$participant) {
                return response()->json(['error' => 'User is not a participant of this event'], 404);
            }

            // guarda o nome do utilizador antes de expulsar
            $kickedUserName = $participant->user_name;

            // remove participante na BD
            $participant->delete();

            //  publica no rabbit 
            $messageDataForChat = [
                'event_id' => $event_id,
                'user_id' => $user_id,
            ];

            // usa a mesma fila de saair do evento
            $this->publishToRabbitMQ('user_left_event', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('lolchat_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('ach_event_join-leave', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('noti_event_join-leave', json_encode($messageDataForChat));

            // pega nos restantes participantes para notificar
            $remainingParticipants = Participant::where('event_id', $event_id)->get(['user_id', 'user_name'])->toArray();

            // prepara a mensagem
            $notificationMessage = [
                'type' => 'new_message',
                'event_id' => $event_id,
                'event_name' => $event->name ?? "Evento $event_id",
                'user_id' => $user_id,
                'user_name' => $kickedUserName,
                'message' => 'A user was kicked from the event',
                'timestamp' => now()->toISOString(),
                'participants' => $remainingParticipants,
            ];

            //  publica na fila notification
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['message' => 'Participant kicked out successfully'], 200); // sucess

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }



    //───────────────────────────────────────────── LISTAR EVENTOS PUBLICO ────────────────────────────────────────────────────────────────────────────────────
// lista eventos criados de qualquer user PUBLICO
    public function eventsByUser(Request $request, $id)
    {
        try {
            // valida token
            $this->validateToken($request);

            // procura eventos onde o criador é o id fornecido
            $events = Event::with('sport')
                ->where('user_id', $id)
                ->get();

            // transforma o evento para o mesmo formato usado em index
            $response = $events->map(function ($event) {
                $rawWeather = is_array($event->weather)
                    ? $event->weather
                    : json_decode($event->weather, true) ?? [];

                $weather = [   // formata dados do weather
                    'app_max_temp' => $rawWeather['app_max_temp'] ?? null,
                    'app_min_temp' => $rawWeather['app_min_temp'] ?? null,
                    'temp' => $rawWeather['temp'] ?? null,
                    'high_temp' => $rawWeather['high_temp'] ?? null,
                    'low_temp' => $rawWeather['low_temp'] ?? null,
                    'description' => $rawWeather['weather']['description'] ?? null,
                ];

                // busca os participantes
                $participants = DB::table('event_user')
                    ->where('event_id', $event->id)
                    ->whereNull('deleted_at')
                    ->get(['user_id', 'user_name', 'rating'])
                    ->map(function ($p) {
                        return [
                            'id' => $p->user_id,
                            'name' => $p->user_name,
                            'rating' => $p->rating,
                        ];
                    });

                return [  // devolve o array formatado
                    'id' => $event->id,
                    'name' => $event->name,
                    'sport' => $event->sport->name ?? null,
                    'starts_at' => $event->starts_at,
                    'place' => $event->place,
                    'status' => $event->status,
                    'max_participants' => $event->max_participants,
                    'latitude' => $event->latitude,
                    'longitude' => $event->longitude,
                    'creator' => [
                        'id' => $event->user_id,
                        'name' => $event->user_name,
                    ],
                    'weather' => $weather,
                    'participants' => $participants,
                ];
            });

            return response()->json($response, 200);   // sucesso
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }



    //───────────────────────────────────────────── DASHBOARD STATS────────────────────────────────────────────────────────────────────────────────────
    public function overview(Request $request)
    {
        //valida token e pega id  e verifica se e admin
        $token = app(EventController::class)->validateToken($request);
        $payload = JWTAuth::setToken($token)->getPayload();
        $userId = $payload->get('sub');
        $isAdmin = $payload->get('is_admin');

        // verifica se é o utilizador ou admin
        $targetUserId = $request->query('user_id', $userId);
        if ($targetUserId != $userId && !$isAdmin) {
            return response()->json(['error' => 'Unauthorized'], 403);
        }

        // ─── calcula intervalos de datas com Carbon ────────────────────────────────
        $now = Carbon::now();
        $startOfWeek = $now->copy()->startOfWeek();
        $endOfWeek = $now->copy()->endOfWeek();
        $startOfWeekLastYr = $startOfWeek->copy()->subYear();
        $endOfWeekLastYr = $endOfWeek->copy()->subYear();

        $startOfMonth = $now->copy()->startOfMonth();
        $endOfMonth = $now->copy()->endOfMonth();
        $startOfLastMonth = $now->copy()->subMonthNoOverflow()->startOfMonth();
        $endOfLastMonth = $now->copy()->subMonthNoOverflow()->endOfMonth();

        // ─── total de eventos em progresso ─────────────────────
        $totalActive = Event::where('user_id', $targetUserId)
            ->where('status', 'in progress')
            ->count();

        // ───  Eventos criados esta semana vs mesma semana ano passado ──────────
        $createdThisWeek = Event::where('user_id', $targetUserId)
            ->whereBetween('created_at', [$startOfWeek, $endOfWeek])
            ->count();

        $createdWeekLastYr = Event::where('user_id', $targetUserId)
            ->whereBetween('created_at', [$startOfWeekLastYr, $endOfWeekLastYr])
            ->count();

        // ─── Participações (joins) e saídas (leaves) este mês ─────────────────────────────
        $joinedThisMonth = Participant::whereHas('event', function ($q) use ($targetUserId) {
            $q->where('user_id', $targetUserId);
        })
            ->whereBetween('created_at', [$startOfMonth, $endOfMonth])
            ->count();

        $leftThisMonth = Participant::onlyTrashed()
            ->whereHas('event', function ($q) use ($targetUserId) {
                $q->where('user_id', $targetUserId);
            })
            ->whereBetween('deleted_at', [$startOfMonth, $endOfMonth])
            ->count();

        $joinedLastMonth = Participant::whereHas('event', function ($q) use ($targetUserId) {
            $q->where('user_id', $targetUserId);
        })
            ->whereBetween('created_at', [$startOfLastMonth, $endOfLastMonth])
            ->count();

        // ─── Top-3 locais e desportos──────────────────────────────────────────────
        $topPlaces = Event::where('user_id', $targetUserId)
            ->select('place', DB::raw('COUNT(*) as total'))
            ->groupBy('place')
            ->orderByDesc('total')
            ->limit(3)
            ->get();

        $topSports = Event::with('sport')
            ->where('user_id', $targetUserId)
            ->select('sport_id', DB::raw('COUNT(*) as total'))
            ->groupBy('sport_id')
            ->orderByDesc('total')
            ->limit(3)
            ->get()
            ->map(function ($row) {
                return [
                    'sport_id' => $row->sport_id,
                    'sport_name' => $row->sport->name ?? null,
                    'total' => $row->total,
                ];
            });

        // ─── Resposta em JSON de todas as estatisticas──────────────────────────────────────
        return response()->json([
            'user_id' => $targetUserId,
            'total_active_activities' => $totalActive,
            'created_this_week' => $createdThisWeek,
            'created_this_week_last_year' => $createdWeekLastYr,
            'participants_joined_this_month' => $joinedThisMonth,
            'participants_left_this_month' => $leftThisMonth,
            'participants_joined_last_month' => $joinedLastMonth,
            'top_locations' => $topPlaces,
            'top_sports' => $topSports,
        ], 200);
    }
    //───────────────────────────────────────────── ADMIN──────────────────────────────────────────────────────────────────────────────────

    /**
     * Admin List all events
     */
    public function listAllEvents(Request $request)
    {
        try {
            // Validate token and check admin privilege
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $isAdmin = $payload->get('is_admin'); // Assuming the payload includes an 'is_admin' field

            if (!$isAdmin) {
                return response()->json(['error' => 'Unauthorized'], 403);
            }

            // Fetch all events with participants and their ratings
            $events = Event::with(['sport'])->get();

            // Transform and return response
            $response = $events->map(function ($event) {
                $participants = DB::table('event_user')
                    ->where('event_id', $event->id)
                    ->get(['user_id', 'rating']);

                return [
                    'id' => $event->id,
                    'name' => $event->name,
                    'sport' => $event->sport->name ?? null,
                    'starts_at' => $event->starts_at,
                    'place' => $event->place,
                    'status' => $event->status,
                    'max_participants' => $event->max_participants,
                    'creator' => [
                        'id' => $event->user_id,
                        'name' => $event->user_name,
                    ],
                    'participants' => $participants->map(function ($participant) {
                        return [
                            'id' => $participant->user_id,
                            'rating' => $participant->rating,
                        ];
                    }),
                ];
            });

            return response()->json($response, 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 500);
        }
    }


    /**
     * Admin delete all events
     */
    public function deleteEventAsAdmin(Request $request, $id)
    {
        try {
            // Validate token and check admin privilege
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $isAdmin = $payload->get('is_admin'); // Assuming the payload includes an 'is_admin' field

            if (!$isAdmin) {
                return response()->json(['error' => 'Unauthorized'], 403);
            }

            // Find the event
            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // Store event ID for publishing to RabbitMQ
            $eventDataForChatDeletion = ['event_id' => $event->id];

            // Delete the event
            $event->delete();

            // Publish a message to RabbitMQ with the event ID (same as in destroy method)
            $this->publishToRabbitMQ('event_deleted', json_encode($eventDataForChatDeletion));

            return response()->json(['message' => 'Event deleted successfully'], 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 500);
        }
    }


    /**
     * Admin concludes event
     */
    public function concludeEvent(Request $request, $id)
    {
        try {
            // Validate token and check admin privileges
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $isAdmin = $payload->get('is_admin');

            if (!$isAdmin) {
                return response()->json(['error' => 'Unauthorized: Admins only'], 403);
            }

            // Find the event
            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // Update the status to 'concluded'
            $event->status = 'concluded';
            $event->save();

            return response()->json(['message' => 'Event status updated to concluded', 'event' => $event], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => 'Failed to conclude event: ' . $e->getMessage()], 500);
        }
    }


    //─────────────────────────────────────────────    obsolete────────────────────────────────────────────────────────────────────────────────────
    public function rateUser(Request $request, $event_id)
    {
        try {
            // Validate token and get authenticated user's ID
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $authUserId = $payload->get('sub'); // Get the authenticated user's ID

            // Validate incoming data
            $validated = $request->validate([
                'user_id' => 'required|exists:event_user,user_id', // Ensure the user being rated exists in event_user
                'rating' => 'required|integer|min:1|max:5',        // Ensure rating is an integer between 1 and 5
            ]);

            // Find the event
            $event = Event::findOrFail($event_id);

            // Check if the event is concluded
            if ($event->status !== 'concluded') {
                return response()->json(['error' => 'Event must be concluded to rate participants'], 400);
            }

            // Check if the authenticated user participated in the event
            if (!Participant::where('event_id', $event_id)->where('user_id', $authUserId)->exists()) {
                return response()->json(['error' => 'You did not participate in this event'], 403);
            }

            // Check if the user being rated participated in the event
            if (!Participant::where('event_id', $event_id)->where('user_id', $validated['user_id'])->exists()) {
                return response()->json(['error' => 'The user being rated did not participate in this event'], 403);
            }

            // Update the rating in the event_user table
            DB::table('event_user')
                ->where('event_id', $event_id)
                ->where('user_id', $validated['user_id'])
                ->update(['rating' => $validated['rating']]);

            return response()->json(['message' => 'Rating updated successfully'], 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 500);
        }
    }



}
