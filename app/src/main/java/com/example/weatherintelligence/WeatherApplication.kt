package com.example.weatherintelligence

import android.app.Application
import android.content.Context
import com.example.weatherintelligence.data.local.WeatherCacheDataSource
import com.example.weatherintelligence.data.local.WeatherCacheDatabase
import com.example.weatherintelligence.data.remote.WeatherApiClient
import com.example.weatherintelligence.data.repository.WeatherRepository
import com.example.weatherintelligence.notifications.WeatherNotifier
import com.example.weatherintelligence.sync.WeatherSyncWorker

class WeatherApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        WeatherNotifier.ensureChannel(this)
        WeatherSyncWorker.schedule(this)
    }
}

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val apiClient = WeatherApiClient()
    private val cacheDatabase = WeatherCacheDatabase(appContext)
    val cacheDataSource = WeatherCacheDataSource(cacheDatabase)
    val repository = WeatherRepository(
        cacheDataSource = cacheDataSource,
        geocodingService = apiClient.geocodingService,
        forecastService = apiClient.forecastService,
        apiKey = BuildConfig.WEATHER_API_KEY,
    )
}
