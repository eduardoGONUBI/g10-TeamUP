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

    private $weatherService;

    public function __construct(WeatherService $weatherService)
    {
        $this->weatherService = $weatherService;
    }
    /**
     * Check if a token is blacklisted using the /get-blacklist endpoint.
     */
    private function isTokenBlacklisted($token)
    {
        $cacheKey = "blacklisted:{$token}";

        return Cache::has($cacheKey);
    }

    /**
     * Process token from Authorization header.
     */
    private function getTokenFromRequest(Request $request)
    {
        $token = $request->header('Authorization');

        if (!$token) {
            throw new \Exception('Token is required.');
        }

        // Strip "Bearer " prefix if present
        if (str_starts_with($token, 'Bearer ')) {
            $token = substr($token, 7);
        }

        return $token;
    }

    /**
     * Validate token and check if blacklisted.
     */
    private function validateToken(Request $request)
    {
        $token = $this->getTokenFromRequest($request);

        if ($this->isTokenBlacklisted($token)) {
            throw new \Exception('Unauthorized: Token is blacklisted.');
        }

        return $token;
    }

    private function publishToRabbitMQ($queueName, $messageBody)
    {
        try {
            $connection = new AMQPStreamConnection('rabbitmq', 5672, 'guest', 'guest');
            $channel = $connection->channel();

            // Declare the queue
            $channel->queue_declare($queueName, false, true, false, false);

            // Create the message
            $msg = new AMQPMessage($messageBody, [
                'delivery_mode' => AMQPMessage::DELIVERY_MODE_PERSISTENT
            ]);

            // Publish the message to the specified queue
            $channel->basic_publish($msg, '', $queueName);



            $channel->close();
            $connection->close();
        } catch (\Exception $e) {

        }
    }




    /**
     * Store a new event.
     */
    public function store(Request $request)
    {
        try {
            $token = $this->validateToken($request);

            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            // Only validate the things the user really submits now
            $validatedData = $request->validate([
                'name' => 'required|string',
                'sport_id' => 'required|exists:sports,id',
                'date' => 'required|date',
                'place' => 'required|string',
                'max_participants' => 'required|integer|min:2',
            ]);

            //
            // ─── Geocode the place ───────────────────────────────────────────────
            //
            $geoKey = config('services.google_maps.key')
                ?? env('GOOGLE_MAPS_GEOCODE_API_KEY');
            if (!$geoKey) {
                throw new \Exception('Missing Google Geocoding API key');
            }

            $geoRes = Http::get('https://maps.googleapis.com/maps/api/geocode/json', [
                'address' => $validatedData['place'],
                'key' => $geoKey,
            ])->json();

             if (($geoRes['status'] ?? '') === 'ZERO_RESULTS') {
            // user typed something Google doesn't recognize
            return response()->json([
                'error' => 'Address not found. Please enter a valid location.'
            ], 422);
        }

        if (($geoRes['status'] ?? '') !== 'OK' ||
            empty($geoRes['results'][0]['geometry']['location'])) {
            $status = $geoRes['status'] ?? 'UNKNOWN';
            return response()->json([
                'error' => "Error while searching for the address: {$status}. Please try again."
            ], 422);
        }

            $loc = $geoRes['results'][0]['geometry']['location'];
            $lat = $loc['lat'];
            $lng = $loc['lng'];

            //
            // ─── Fetch weather for that lat/lng + date ────────────────────────────
            //
            $eventDate = Carbon::parse($validatedData['date'])->format('Y-m-d');
            $weatherData = $this->weatherService
                ->getForecastForDate($lat, $lng, $eventDate);

            //
            // ─── Finally, create the event ───────────────────────────────────────
            //
            $event = Event::create([
                'name' => $validatedData['name'],
                'sport_id' => $validatedData['sport_id'],
                'date' => $validatedData['date'],
                'place' => $validatedData['place'],
                'user_id' => $userId,
                'user_name' => $userName,
                'status' => 'in progress',
                'max_participants' => $validatedData['max_participants'],
                'latitude' => $lat,
                'longitude' => $lng,
                'weather' => json_encode($weatherData),
            ]);

            // auto-join creator
            Participant::create([
                'event_id' => $event->id,
                'user_id' => $userId,
                'user_name' => $userName,
            ]);

            // push to RabbitMQ
            $msg = [
                'event_id' => $event->id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'User joined the event',
            ];
            $this->publishToRabbitMQ('event_joined', json_encode($msg));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($msg));

            return response()->json([
                'message' => 'Evento criado com sucesso!',
                'event' => $event,
            ], 201);

        } catch (\Exception $e) {
            // if logging still fails, you'll see the error returned here
            return response()->json(['error' => $e->getMessage()], 400);
        }
    }


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

     public function show(Request $request, $id)
    {
        // reuse your validateToken helper if you need auth
        $token = $this->validateToken($request);

        $event = Event::findOrFail($id);

        // decode the JSON string into an array
        $raw = is_array($event->weather)
             ? $event->weather
             : json_decode($event->weather, true) ?? [];

        // pick out the bits your UI needs
        $weather = [
            'temp'      => $raw['temp']      ?? null,
            'high_temp' => $raw['high_temp'] ?? null,
            'low_temp'  => $raw['low_temp']  ?? null,
            'description' => $raw['weather']['description'] ?? null,
        ];

        return response()->json([
            'id'               => $event->id,
            'name'             => $event->name,
            'sport'            => $event->sport->name ?? null,
            'date'             => $event->date,
            'place'            => $event->place,
            'status'           => $event->status,
            'max_participants' => $event->max_participants,
            'latitude'         => $event->latitude,
            'longitude'        => $event->longitude,
            'user_id'          => $event->user_id,
            'creator'          => [
                'id'   => $event->user_id,
                'name' => $event->user_name,
            ],
            'weather'          => $weather,
        ], 200);
    }
    /**
     * Fetch events for a user.
     */
    public function index(Request $request)
    {
        try {
            $token = $this->validateToken($request);

            // Get authenticated user ID from token payload
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            // Fetch only events created by the authenticated user
            $events = Event::with('sport')
                ->where('user_id', $userId)
                ->get();

            // Transform and return events with participant ratings
            $response = $events->map(function ($event) {
                // Fetch participants and their ratings
                $participants = DB::table('event_user')
                    ->where('event_id', $event->id)
                    ->get(['user_id', 'user_name', 'rating']);

                return [
                    'id' => $event->id,
                    'name' => $event->name,
                    'sport' => $event->sport->name ?? null,
                    'date' => $event->date,
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
                            'name' => $participant->user_name,
                            'rating' => $participant->rating,
                        ];
                    }),
                ];
            });

            return response()->json($response, 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    /**
     * Delete an event.
     */
    /**
     * Delete an event.
     */
    /**
     * Delete an event.
     */
    public function destroy(Request $request, $id)
    {
        try {
            $token = $this->validateToken($request);

            // Get authenticated user ID from token payload
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            // Find the event
            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // Check if the user is the creator of the event
            if ((int) $event->user_id !== (int) $userId) {
                return response()->json(['error' => 'Unauthorized'], 403);
            }

            // Store event ID for publishing to RabbitMQ
            $eventDataForChatDeletion = ['event_id' => $event->id];

            // Delete the event
            $event->delete();

            // Publish a message to RabbitMQ with the event ID
            $this->publishToRabbitMQ('event_deleted', json_encode($eventDataForChatDeletion));

            return response()->json(['message' => 'Event deleted successfully'], 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    /**
     * Update an event.
     */
    public function update(Request $request, $id)
    {
        try {
            $token = $this->validateToken($request);
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            if ((int) $event->user_id !== (int) $userId) {
                return response()->json(['error' => 'Unauthorized'], 403);
            }

            $validatedData = $request->validate([
                'name' => 'string|nullable',
                'sport_id' => 'exists:sports,id|nullable',
                'date' => 'date|nullable',
                'place' => 'string|nullable',
                'max_participants' => 'integer|min:2|nullable',
                'status' => 'in:in progress,concluded|nullable',
            ]);

            $event->update($validatedData);

            // Fetch all participants of the updated event
            $participants = Participant::where('event_id', $id)
                ->distinct()
                ->get(['user_id', 'user_name'])
                ->toArray();

            // Prepare the notification message
            $notificationMessage = [
                'type' => 'new_message',
                'event_id' => $id,
                'event_name' => $event->name ?? "Evento $id",
                'user_id' => $userId,
                'user_name' => $this->getUserNameFromToken($token), // Implement this method if needed
                'message' => 'Event was Updated',
                'timestamp' => now()->toISOString(),
                'participants' => $participants,
            ];

            // Publish the message to the RabbitMQ queue named 'notification'
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['message' => 'Event updated successfully', 'event' => $event], 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

    /**
     * Optionally, define this helper method if you need the user_name from the token.
     * If the token payload contains 'name', you can retrieve it similarly to user_id.
     */
    private function getUserNameFromToken($token)
    {
        $payload = JWTAuth::setToken($token)->getPayload();
        return $payload->get('name') ?? null;
    }


    /**
     * Join an event.
     */
    public function join(Request $request, $id)
    {
        try {
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // Prevent joining a concluded event
            if ($event->status === 'concluded') {
                return response()->json(['error' => 'This event has concluded. You can no longer join.'], 400);
            }

            if ($event->user_id === $userId) {
                return response()->json(['error' => 'You cannot join your own event'], 400);
            }

            $currentParticipantCount = Participant::where('event_id', $id)->count();

            if ($currentParticipantCount >= $event->max_participants) {
                return response()->json(['error' => 'Event is full'], 400);
            }

            if (Participant::where('event_id', $id)->where('user_id', $userId)->exists()) {
                return response()->json(['error' => 'You have already joined this event'], 400);
            }

            // Add the user as a participant
            Participant::create([
                'event_id' => $id,
                'user_id' => $userId,
                'user_name' => $userName,
            ]);


            // Fetch all participants to notify
            $participants = Participant::where('event_id', $id)->get(['user_id', 'user_name'])->toArray();

            // Prepare the notification message without calling ->toArray() again on $participants
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

            // Publish the message to the RabbitMQ queue named 'notification'
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));


            // Publish a message specifically for the chat service queue after a user joins the event.
            $messageDataForChat = [
                'event_id' => $id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'User joined the event'
            ];

            // Publish this to the 'event_joined' queue (or whichever queue the chat microservice listens to)
            $this->publishToRabbitMQ('event_joined', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($messageDataForChat));



            return response()->json(['message' => 'You have successfully joined the event'], 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    public function participants(Request $request, $id)
    {
        try {
            /* ─── 1. Autenticação ──────────────────────────────────────────────── */
            $token = $this->validateToken($request);
            $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

            /* ─── 2. Evento & permissão ────────────────────────────────────────── */
            $event = Event::find($id);
            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

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

            /* ─── 3. Participantes (apenas event_user) ────────────────────────── */
            $participants = DB::table('event_user')
                ->where('event_id', $id)
                ->get(['user_id', 'user_name', 'rating'])   // same columns as antes
                ->map(function ($p) {
                    return [
                        'id' => $p->user_id,
                        'name' => $p->user_name,
                        'rating' => $p->rating,
                        'avatar_url' => null,          // falta avatar → React usa default
                    ];
                });

            /* ─── 4. Resposta ─────────────────────────────────────────────────── */
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



    /**
     * Fetch user events.
     */
    public function userEvents(Request $request)
{
    try {
        $token = $this->validateToken($request);
        $userId = JWTAuth::setToken($token)->getPayload()->get('sub');

        $events = Event::with('sport')
            ->where('user_id', $userId)
            ->orWhereHas('participants', function ($query) use ($userId) {
                $query->where('user_id', $userId);
            })
            ->get();

        $response = $events->map(function ($event) {
            // Decode weather if it's a string
            $weatherData = is_array($event->weather)
                ? $event->weather
                : json_decode($event->weather, true) ?? [];

            // Fetch participants
            $participants = DB::table('event_user')
                ->where('event_id', $event->id)
                ->whereNull('deleted_at')
                ->get(['user_id', 'user_name', 'rating']);

            return [
                'id' => $event->id,
                'name' => $event->name,
                'sport' => $event->sport->name ?? null,
                'date' => $event->date,
                'place' => $event->place,
                'status' => $event->status,
                'max_participants' => $event->max_participants,
                'latitude' => $event->latitude,
                'longitude' => $event->longitude,
                'creator' => [
                    'id' => $event->user_id,
                    'name' => $event->user_name,
                ],
                'weather' => [
                    'app_max_temp' => $weatherData['app_max_temp'] ?? 'N/A',
                    'app_min_temp' => $weatherData['app_min_temp'] ?? 'N/A',
                    'temp' => $weatherData['temp'] ?? 'N/A',
                    'high_temp' => $weatherData['high_temp'] ?? 'N/A',
                    'low_temp' => $weatherData['low_temp'] ?? 'N/A',
                    'description' => $weatherData['weather']['description'] ?? 'N/A',
                ],
                'participants' => $participants->map(function ($participant) {
                    return [
                        'id' => $participant->user_id,
                        'name' => $participant->user_name,
                        'rating' => $participant->rating,
                    ];
                }),
            ];
        });

        return response()->json($response, 200);
    } catch (\Exception $e) {
        return response()->json(['error' => $e->getMessage()], 401);
    }
}


    /**
     * Search for events.
     */
    public function search(Request $request)
    {
        try {
            $token = $this->validateToken($request);

            $query = Event::query();

            if ($request->has('id')) {
                $query->where('id', $request->input('id'));
            }

            if ($request->has('name')) {
                $query->where('name', 'like', '%' . $request->input('name') . '%');
            }

            if ($request->has('date')) {
                $query->where('date', $request->input('date'));
            }

            if ($request->has('place')) {
                $query->where('place', 'like', '%' . $request->input('place') . '%');
            }

            $events = $query->get();

            if ($events->isEmpty()) {
                return response()->json(['message' => 'No events found'], 200);
            }

            return response()->json($events, 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }

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
                    'date' => $event->date,
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

    /**
     * Creator concludes / ends an event.
     *
     * PUT /events/{id}/conclude        (see routes file below)
     */
    public function concludeByCreator(Request $request, $id)
    {
        try {
            // ─── Auth & ownership ─────────────────────────────────────────────────────
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name');

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

            // ─── Persist new state ────────────────────────────────────────────────────
            $event->status = 'concluded';
            $event->save();

            // ─── Notify other services via RabbitMQ ───────────────────────────────────
            // (1) A dedicated queue for the event‑lifecycle workflow
            $lifecycleMessage = [
                'event_id' => $event->id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'Event concluded by creator',
            ];
            $this->publishToRabbitMQ('event_concluded', json_encode($lifecycleMessage));
            $this->publishToRabbitMQ('rating_event_concluded', json_encode($lifecycleMessage));

            // (2) A chat/notification fan‑out just like join(), leave(), etc.
            $participants = Participant::where('event_id', $id)
                ->get(['user_id', 'user_name'])
                ->toArray();

            $notification = [
                'type' => 'new_message',
                'event_id' => $event->id,
                'event_name' => $event->name,
                'user_id' => $userId,
                'user_name' => $userName,
                'message' => 'The event has ended',
                'timestamp' => now()->toISOString(),
                'participants' => $participants,
            ];
            $this->publishToRabbitMQ('notification', json_encode($notification));

            return response()->json([
                'message' => 'Event concluded successfully',
                'event' => $event,
            ], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    /**
     * User Leaves event
     */
    public function leave(Request $request, $id)
    {
        try {
            // Validate the token and retrieve the user info
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $userId = $payload->get('sub');
            $userName = $payload->get('name'); // Add userName for the notification message

            // Find the event by ID
            $event = Event::find($id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // Check if the user is a participant of the event
            $participant = Participant::where('event_id', $id)->where('user_id', $userId)->first();

            if (!$participant) {
                return response()->json(['error' => 'You are not a participant of this event'], 400);
            }

            // Delete the participant record to leave the event
            $participant->delete();

            // Publish a message to RabbitMQ to inform the chat microservice
            $messageDataForChat = [
                'event_id' => $id,
                'user_id' => $userId
            ];

            $this->publishToRabbitMQ('user_left_event', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($messageDataForChat));

            // Fetch all remaining participants to notify
            $remainingParticipants = Participant::where('event_id', $id)->get(['user_id', 'user_name'])->toArray();


            // Prepare the notification message without calling ->toArray() again on $participants
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

            // Publish the message to the RabbitMQ queue named 'notification'
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['message' => 'You have successfully left the event'], 200);
        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


    /**
     * Creator kicks user from event
     */
    public function kickParticipant(Request $request, $event_id, $user_id)
    {
        try {
            // Validate the token and get the authenticated user's info
            $token = $this->validateToken($request);
            $payload = JWTAuth::setToken($token)->getPayload();
            $currentUserId = $payload->get('sub');

            // Find the event
            $event = Event::find($event_id);

            if (!$event) {
                return response()->json(['error' => 'Event not found'], 404);
            }

            // Check if the current user is the creator of the event
            if ((int) $event->user_id !== (int) $currentUserId) {
                return response()->json(['error' => 'You are not the creator of this event'], 403);
            }

            // Check if the user to be kicked is a participant
            $participant = Participant::where('event_id', $event_id)
                ->where('user_id', $user_id)
                ->first();

            if (!$participant) {
                return response()->json(['error' => 'User is not a participant of this event'], 404);
            }

            // Store the participant's name before deleting
            $kickedUserName = $participant->user_name;

            // Kick the participant out (delete the participant record)
            $participant->delete();

            // Publish a message to RabbitMQ to inform the chat microservice that the user left (or was kicked)
            $messageDataForChat = [
                'event_id' => $event_id,
                'user_id' => $user_id,
            ];

            // Using the same queue as the "leave" method
            $this->publishToRabbitMQ('user_left_event', json_encode($messageDataForChat));
            $this->publishToRabbitMQ('chat_event_join-leave', json_encode($messageDataForChat));

            // Fetch all remaining participants to notify
            $remainingParticipants = Participant::where('event_id', $event_id)->get(['user_id', 'user_name'])->toArray();

            // Prepare the notification message
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

            // Publish the notification
            $this->publishToRabbitMQ('notification', json_encode($notificationMessage));

            return response()->json(['message' => 'Participant kicked out successfully'], 200);

        } catch (\Exception $e) {
            return response()->json(['error' => $e->getMessage()], 401);
        }
    }


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

    /**
     * GET /stats
     * Optional ?user_id=X (admins only) – otherwise stats for the auth user.
     */
    public function overview(Request $request)
    {
        // ─── Auth / optional impersonation ───────────────────────────────────────
        $token = app(EventController::class)->validateToken($request); // reuse helper
        $payload = JWTAuth::setToken($token)->getPayload();
        $userId = $payload->get('sub');
        $isAdmin = $payload->get('is_admin');

        $targetUserId = $request->query('user_id', $userId);
        if ($targetUserId != $userId && !$isAdmin) {
            return response()->json(['error' => 'Unauthorized'], 403);
        }

        // ─── Date helpers (all Carbon, local tz) ────────────────────────────────
        $now = Carbon::now();
        $startOfWeek = $now->copy()->startOfWeek();
        $endOfWeek = $now->copy()->endOfWeek();
        $startOfWeekLastYr = $startOfWeek->copy()->subYear();
        $endOfWeekLastYr = $endOfWeek->copy()->subYear();

        $startOfMonth = $now->copy()->startOfMonth();
        $endOfMonth = $now->copy()->endOfMonth();
        $startOfLastMonth = $now->copy()->subMonthNoOverflow()->startOfMonth();
        $endOfLastMonth = $now->copy()->subMonthNoOverflow()->endOfMonth();

        // ─── Query 1: total active activities for that user ─────────────────────
        $totalActive = Event::where('user_id', $targetUserId)
            ->where('status', 'in progress')
            ->count();

        // ─── Query 2-3: events created this week / same week last year ──────────
        $createdThisWeek = Event::where('user_id', $targetUserId)
            ->whereBetween('created_at', [$startOfWeek, $endOfWeek])
            ->count();

        $createdWeekLastYr = Event::where('user_id', $targetUserId)
            ->whereBetween('created_at', [$startOfWeekLastYr, $endOfWeekLastYr])
            ->count();

        // ─── participant-based queries (join/leave) ─────────────────────────────
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

        // ─── Top-3 places all time ──────────────────────────────────────────────
        $topPlaces = Event::where('user_id', $targetUserId)
            ->select('place', DB::raw('COUNT(*) as total'))
            ->groupBy('place')
            ->orderByDesc('total')
            ->limit(3)
            ->get();

        // ─── Top-3 sports all time ──────────────────────────────────────────────
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

        // ─── Return nicely structured JSON ──────────────────────────────────────
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



}
