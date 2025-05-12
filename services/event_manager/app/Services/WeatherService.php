<?php

namespace App\Services;

use Illuminate\Support\Facades\Http;

class WeatherService
{
    private $apiKey;
    private $baseUrl;

    public function __construct()
    {
        // Carregar a chave da API e o URL base a partir do ficheiro .env
        $this->apiKey = env('WEATHERBIT_API_KEY');
        $this->baseUrl = 'https://api.weatherbit.io/v2.0/';
    }

    /**
     * Obter a previsão meteorológica para uma data específica.
     */
    public function getForecastForDate($latitude, $longitude, $eventDate)
    {
        // Chamar o endpoint da API Weatherbit para previsões diárias (16 dias)
        $response = Http::get($this->baseUrl . 'forecast/daily', [
            'lat'   => $latitude,
            'lon'   => $longitude,
            'key'   => $this->apiKey,
            'units' => 'M', // Métrico (Celsius)
        ]);

        if ($response->successful()) {
            $forecastData = $response->json();

            // Filtrar a previsão para a data especificada
            $filteredForecasts = array_filter($forecastData['data'], function ($forecast) use ($eventDate) {
                return $forecast['datetime'] === $eventDate;
            });

            if (!empty($filteredForecasts)) {
                return array_values($filteredForecasts)[0]; // Retornar a previsão mais próxima
            }

            throw new \Exception('No forecast data available for the specified date.');
        } else {
            // Capturar o status code e o corpo da resposta para maior clareza do erro
            $statusCode = $response->status();
            $responseBody = $response->body();
            $errorMessage = "Weatherbit Error [{$statusCode}]: {$responseBody}";
            throw new \Exception($errorMessage);
        }
    }
}
