package com.example.weatherintelligence.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class WeatherApiClient {
    val geocodingService: GeocodingService by lazy {
        retrofit.create(GeocodingService::class.java)
    }

    val forecastService: ForecastService by lazy {
        retrofit.create(ForecastService::class.java)
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
    }
}
