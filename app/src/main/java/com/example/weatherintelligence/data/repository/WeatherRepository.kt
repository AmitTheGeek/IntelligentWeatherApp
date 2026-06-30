package com.example.weatherintelligence.data.repository

import com.example.weatherintelligence.data.local.WeatherCacheDataSource
import com.example.weatherintelligence.data.local.WeatherCacheKeys
import com.example.weatherintelligence.data.remote.DailyDto
import com.example.weatherintelligence.data.remote.ForecastResponse
import com.example.weatherintelligence.data.remote.ForecastService
import com.example.weatherintelligence.data.remote.GeocodingResult
import com.example.weatherintelligence.data.remote.GeocodingService
import com.example.weatherintelligence.data.remote.HourlyDto
import com.example.weatherintelligence.domain.CachedCity
import com.example.weatherintelligence.domain.CurrentWeather
import com.example.weatherintelligence.domain.DailyForecast
import com.example.weatherintelligence.domain.HourlyForecast
import com.example.weatherintelligence.domain.RefreshPolicy
import com.example.weatherintelligence.domain.WeatherCode
import com.example.weatherintelligence.domain.WeatherSnapshot
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class WeatherRepository(
    private val cacheDataSource: WeatherCacheDataSource,
    private val geocodingService: GeocodingService,
    private val forecastService: ForecastService,
    private val refreshPolicy: RefreshPolicy = RefreshPolicy(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun observeWeather(query: String): Flow<WeatherSnapshot?> =
        cacheDataSource.observeWeather(query.toQueryKey())

    suspend fun refresh(query: String, force: Boolean = false): WeatherSnapshot = withContext(ioDispatcher) {
        val safeQuery = query.takeIf { it.isNotBlank() } ?: DEFAULT_CITY
        val queryKey = safeQuery.toQueryKey()
        val cached = cacheDataSource.getWeather(queryKey)

        if (cached != null && !force && refreshPolicy.isFresh(cached.updatedAtEpochMillis)) {
            return@withContext cached
        }

        val location = cached?.toLocation() ?: geocode(safeQuery)
        refreshLocation(queryKey = queryKey, location = location, cachedFallback = cached)
    }

    suspend fun refreshCachedCity(city: CachedCity, force: Boolean = false): WeatherSnapshot =
        withContext(ioDispatcher) {
            val cached = cacheDataSource.getWeather(city.queryKey)
            if (cached != null && !force && refreshPolicy.isFresh(cached.updatedAtEpochMillis)) {
                return@withContext cached
            }

            refreshLocation(
                queryKey = city.queryKey,
                location = city.toLocation(),
                cachedFallback = cached,
            )
        }

    suspend fun trackedCities(): List<CachedCity> = cacheDataSource.getTrackedCities()

    private suspend fun refreshLocation(
        queryKey: String,
        location: WeatherLocation,
        cachedFallback: WeatherSnapshot?,
    ): WeatherSnapshot {
        return try {
            val response = forecastService.forecast(
                latitude = location.latitude,
                longitude = location.longitude,
            )
            val snapshot = response.toSnapshot(
                queryKey = queryKey,
                location = location,
                nowEpochMillis = System.currentTimeMillis(),
            )
            cacheDataSource.upsertWeather(snapshot)
            snapshot
        } catch (error: Exception) {
            cachedFallback ?: throw error
        }
    }

    private suspend fun geocode(query: String): WeatherLocation {
        val result = geocodingService.searchCity(query).results.firstOrNull()
            ?: throw IllegalArgumentException("No location found for \"$query\"")
        return result.toLocation()
    }

    private fun String.toQueryKey(): String = WeatherCacheKeys.fromQuery(this.ifBlank { DEFAULT_CITY })

    private fun WeatherSnapshot.toLocation(): WeatherLocation = WeatherLocation(
        cityName = cityName,
        country = country,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
    )

    private fun CachedCity.toLocation(): WeatherLocation = WeatherLocation(
        cityName = cityName,
        country = country,
        latitude = latitude,
        longitude = longitude,
        timezone = timezone,
    )

    private fun GeocodingResult.toLocation(): WeatherLocation {
        val displayCountry = listOfNotNull(adminArea, country)
            .distinct()
            .joinToString(", ")
            .ifBlank { "Unknown region" }
        return WeatherLocation(
            cityName = name,
            country = displayCountry,
            latitude = latitude,
            longitude = longitude,
            timezone = timezone ?: "auto",
        )
    }

    private fun ForecastResponse.toSnapshot(
        queryKey: String,
        location: WeatherLocation,
        nowEpochMillis: Long,
    ): WeatherSnapshot {
        val currentDto = current ?: throw IllegalStateException("Weather response is missing current conditions.")
        val code = currentDto.weatherCode ?: 3
        val currentWeather = CurrentWeather(
            temperatureC = currentDto.temperature ?: 0.0,
            feelsLikeC = currentDto.apparentTemperature ?: currentDto.temperature ?: 0.0,
            humidityPercent = currentDto.humidity ?: 0,
            windKph = currentDto.windSpeed ?: 0.0,
            gustKph = currentDto.windGusts ?: currentDto.windSpeed ?: 0.0,
            weatherCode = code,
            condition = WeatherCode.labelFor(code),
        )

        return WeatherSnapshot(
            queryKey = queryKey,
            cityName = location.cityName,
            country = location.country,
            latitude = location.latitude,
            longitude = location.longitude,
            timezone = timezone ?: location.timezone,
            current = currentWeather,
            hourly = hourly.toHourlyForecasts(currentWeather).take(24),
            daily = daily.toDailyForecasts(),
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    private fun HourlyDto?.toHourlyForecasts(current: CurrentWeather): List<HourlyForecast> {
        if (this == null) return emptyList()
        val count = minOf(time.size, temperature.size, precipitationProbability.size, weatherCode.size, windSpeed.size)
        return (0 until count).map { index ->
            val code = weatherCode[index] ?: current.weatherCode
            HourlyForecast(
                timeIso = time[index],
                temperatureC = temperature[index] ?: current.temperatureC,
                precipitationProbability = precipitationProbability[index] ?: 0,
                windKph = windSpeed[index] ?: current.windKph,
                weatherCode = code,
            )
        }
    }

    private fun DailyDto?.toDailyForecasts(): List<DailyForecast> {
        if (this == null) return emptyList()
        val count = minOf(
            time.size,
            temperatureMax.size,
            temperatureMin.size,
            precipitationProbability.size,
            weatherCode.size,
            windSpeed.size,
        )
        return (0 until count).map { index ->
            DailyForecast(
                dateIso = time[index],
                minTemperatureC = temperatureMin[index] ?: 0.0,
                maxTemperatureC = temperatureMax[index] ?: 0.0,
                precipitationProbability = precipitationProbability[index] ?: 0,
                windKph = windSpeed[index] ?: 0.0,
                weatherCode = weatherCode[index] ?: 3,
            )
        }
    }

    private data class WeatherLocation(
        val cityName: String,
        val country: String,
        val latitude: Double,
        val longitude: Double,
        val timezone: String,
    )

    companion object {
        const val DEFAULT_CITY = "New York"
    }
}
