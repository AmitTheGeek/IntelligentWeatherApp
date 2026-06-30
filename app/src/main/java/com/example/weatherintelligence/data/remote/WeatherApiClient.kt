package com.example.weatherintelligence.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherApiClient {
    val geocodingService: GeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl(GEOCODING_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }

    val forecastService: ForecastService by lazy {
        Retrofit.Builder()
            .baseUrl(FORECAST_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ForecastService::class.java)
    }

    private companion object {
        const val GEOCODING_BASE_URL = "https://geocoding-api.open-meteo.com/"
        const val FORECAST_BASE_URL = "https://api.open-meteo.com/"
    }
}
