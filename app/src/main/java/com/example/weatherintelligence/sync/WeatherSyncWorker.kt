package com.example.weatherintelligence.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.weatherintelligence.WeatherApplication
import com.example.weatherintelligence.data.repository.WeatherRepository
import com.example.weatherintelligence.domain.AlertSeverity
import com.example.weatherintelligence.notifications.WeatherNotifier
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException

class WeatherSyncWorker(
    appContext: Context,
    workerParameters: WorkerParameters,
) : CoroutineWorker(appContext, workerParameters) {
    override suspend fun doWork(): Result {
        val app = applicationContext as WeatherApplication
        val repository = app.container.repository
        val notifier = WeatherNotifier(applicationContext)

        return try {
            val trackedCities = repository.trackedCities()
            if (trackedCities.isEmpty()) {
                val snapshot = repository.refresh(WeatherRepository.DEFAULT_CITY)
                notifier.notifyIfNeeded(snapshot)
            } else {
                trackedCities.forEach { city ->
                    val snapshot = repository.refreshCachedCity(city)
                    notifier.notifyIfNeeded(snapshot)
                }
            }
            Result.success()
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Result.retry()
        } catch (error: Exception) {
            Result.failure()
        }
    }

    private fun WeatherNotifier.notifyIfNeeded(snapshot: com.example.weatherintelligence.domain.WeatherSnapshot) {
        if (snapshot.alert.severity != AlertSeverity.NORMAL) {
            showSevereWeather(snapshot)
        }
    }

    companion object {
        private const val WORK_NAME = "periodic_weather_sync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<WeatherSyncWorker>(
                repeatInterval = 30,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }
    }
}
