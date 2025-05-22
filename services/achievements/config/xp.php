<?php

return [

    /*
    |--------------------------------------------------------------------------
    | XP granted for each event a user completes
    |--------------------------------------------------------------------------
    */
    'events' => 25,

    /*
    |--------------------------------------------------------------------------
    | XP granted for each achievement code
    |--------------------------------------------------------------------------
    */
    'achievements' => [
        'first_event'       => 50,
        'five_events'       => 75,
        'ten_events'        => 100,
        'twentyfive_events' => 150,
        'hundred_events'    => 300,
    ],

    /*
    |--------------------------------------------------------------------------
    | XP → Level curve (index = level, value = minimum total XP)
    | Feel free to tweak or remove this if you hard-code the curve elsewhere.
    |--------------------------------------------------------------------------
    */
    'level_cuts' => [0, 100, 300, 600, 1000, 1500, 2100, 2800, 3600],
];
