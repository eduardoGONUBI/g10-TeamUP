<?php

namespace App\Http\Controllers;

use App\Models\Sport;
use Illuminate\Http\Request;

class SportsController extends Controller
{
    /**
     * Return all sports (id + name).
     */
    public function index()
    {
        // optionally sort by name
        $sports = Sport::orderBy('name')
                       ->get(['id','name']);

        return response()->json($sports, 200);
    }
}