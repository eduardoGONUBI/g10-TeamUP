<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;

class WeatherService
{
    private $apiKey;
    private $baseUrl;

    public function __construct()
    {
        $this->apiKey  = env('WEATHERBIT_API_KEY');
        $this->baseUrl = 'https://api.weatherbit.io/v2.0/';
    }

    /**
     * Obter a previsão meteorológica para uma data específica.
     *
     * @param float  $latitude
     * @param float  $longitude
     * @param string $eventDate  // formato YYYY-MM-DD
     *
     * @return array
     * @throws \Exception
     */
    public function getForecastForDate($latitude, $longitude, $eventDate)
    {
        $response = Http::get($this->baseUrl . 'forecast/daily', [
            'lat'   => $latitude,
            'lon'   => $longitude,
            'key'   => $this->apiKey,
            'units' => 'M', // Métrico (Celsius)
        ]);

        if (! $response->successful()) {
            throw new \Exception('Falha ao obter dados meteorológicos da Weatherbit.');
        }

        $forecastData = $response->json()['data'] ?? [];
        $filtered     = array_filter($forecastData, fn($f) => $f['datetime'] === $eventDate);

        if (empty($filtered)) {
            throw new \Exception('Não há previsão disponível para a data especificada.');
        }

        return array_values($filtered)[0];
    }
}
