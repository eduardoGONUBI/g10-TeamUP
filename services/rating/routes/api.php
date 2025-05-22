<?php

use Illuminate\Http\Request;
use Illuminate\Support\Facades\Route;
use App\Http\Controllers\ChatController;

/*
|--------------------------------------------------------------------------
| API Routes
|--------------------------------------------------------------------------
|
| Here is where you can register API routes for your application. These
| routes are loaded by the RouteServiceProvider within the "api" middleware
| group. Enjoy building your API!
|
*/

// Route to send a message to a specific event
Route::post('/sendMessage/{id}', [ChatController::class, 'sendMessage']);

// Route to fetch all messages for a specific event
Route::get('/fetchMessages/{id}', [ChatController::class, 'fetchMessages']);

Route::get('/concludedEvents', [ChatController::class, 'listConcludedEvents']);

Route::post('/events/{event_id}/rate', [ChatController::class, 'rateUser']);

Route::get('/userAverage/{id}', [ChatController::class, 'showUserAverage']);

Route::post('/events/{event_id}/feedback', [ChatController::class, 'giveFeedback']);

Route::get('/fbi/{id}', [ChatController::class, 'showReputation']);
