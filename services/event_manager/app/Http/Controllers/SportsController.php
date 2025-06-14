<?php

namespace App\Http\Controllers;

use App\Models\Sport;
use Illuminate\Http\Request;

class SportsController extends Controller
{
    // devolve todos os desportos 
    public function index()
    {
        $sports = Sport::orderBy('name')
            ->get(['id', 'name']);

        return response()->json($sports, 200);
    }
}