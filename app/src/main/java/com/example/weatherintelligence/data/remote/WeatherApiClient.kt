package com.example.weatherintelligence.data.remote

import com.example.weatherintelligence.BuildConfig
import okhttp3.OkHttpClient
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
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(SafeNetworkLoggingInterceptor(enabled = BuildConfig.DEBUG))
            .build()
    }

    private companion object {
        const val BASE_URL = "https://api.openweathermap.org/"
    }
}
