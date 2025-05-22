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

Route::get('/achievements/{id}', [ChatController::class, 'listAchievements']);

// NEW – returns { xp, level } or nulls if user never appeared in any event
Route::get('/profile/{id}',       [ChatController::class, 'getProfile']);
