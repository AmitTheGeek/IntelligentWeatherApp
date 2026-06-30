package com.example.weatherintelligence.data.repository

import com.example.weatherintelligence.data.local.WeatherCacheDataSource
import com.example.weatherintelligence.data.local.WeatherCacheKeys
import com.example.weatherintelligence.data.remote.DailyDto
import com.example.weatherintelligence.data.remote.ForecastResponse
import com.example.weatherintelligence.data.remote.ForecastService
import com.example.weatherintelligence.data.remote.GeocodingResult
import com.example.weatherintelligence.data.remote.GeocodingService
import com.example.weatherintelligence.data.remote.HourlyDto
import com.example.weatherintelligence.data.remote.WeatherConditionDto
import com.example.weatherintelligence.domain.CachedCity
import com.example.weatherintelligence.domain.CurrentWeather
import com.example.weatherintelligence.domain.DailyForecast
import com.example.weatherintelligence.domain.HourlyForecast
import com.example.weatherintelligence.domain.OpenWeatherCode
import com.example.weatherintelligence.domain.RefreshPolicy
import com.example.weatherintelligence.domain.WeatherSnapshot
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class WeatherRepository(
    private val cacheDataSource: WeatherCacheDataSource,
    private val geocodingService: GeocodingService,
    private val forecastService: ForecastService,
    private val apiKey: String,
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
                apiKey = requireApiKey(),
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
        val result = geocodingService.searchCity(query = query, apiKey = requireApiKey()).firstOrNull()
            ?: throw IllegalArgumentException("No location found for \"$query\"")
        return result.toLocation()
    }

    private fun requireApiKey(): String {
        val trimmedApiKey = apiKey.trim()
        check(trimmedApiKey.isNotEmpty()) {
            "Missing WEATHER_API_KEY. Add it to local.properties before running the app."
        }
        return trimmedApiKey
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
        val displayCountry = listOfNotNull(state, country)
            .distinct()
            .joinToString(", ")
            .ifBlank { "Unknown region" }
        return WeatherLocation(
            cityName = name,
            country = displayCountry,
            latitude = lat,
            longitude = lon,
            timezone = "UTC",
        )
    }

    private fun ForecastResponse.toSnapshot(
        queryKey: String,
        location: WeatherLocation,
        nowEpochMillis: Long,
    ): WeatherSnapshot {
        val currentDto = current ?: throw IllegalStateException("Weather response is missing current conditions.")
        val zoneId = timezone.zoneIdOrUtc()
        val providerCode = currentDto.weather.primaryProviderCode()
        val code = OpenWeatherCode.normalize(providerCode)
        val currentWeather = CurrentWeather(
            temperatureC = currentDto.temp ?: 0.0,
            feelsLikeC = currentDto.feelsLike ?: currentDto.temp ?: 0.0,
            humidityPercent = currentDto.humidity ?: 0,
            windKph = currentDto.windSpeed.metersPerSecondToKph(),
            gustKph = currentDto.windGust.metersPerSecondToKph().takeIf { it > 0.0 }
                ?: currentDto.windSpeed.metersPerSecondToKph(),
            weatherCode = code,
            condition = OpenWeatherCode.labelFor(providerCode, currentDto.weather.firstOrNull()?.description),
        )

        return WeatherSnapshot(
            queryKey = queryKey,
            cityName = location.cityName,
            country = location.country,
            latitude = lat ?: location.latitude,
            longitude = lon ?: location.longitude,
            timezone = timezone ?: location.timezone,
            current = currentWeather,
            hourly = hourly.toHourlyForecasts(currentWeather, zoneId).take(24),
            daily = daily.toDailyForecasts(zoneId),
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    private fun List<HourlyDto>.toHourlyForecasts(
        current: CurrentWeather,
        zoneId: ZoneId,
    ): List<HourlyForecast> {
        return map { forecast ->
            val providerCode = forecast.weather.firstOrNull()?.id
            val code = providerCode?.let(OpenWeatherCode::normalize) ?: current.weatherCode
            HourlyForecast(
                timeIso = forecast.dt.toIsoDateTime(zoneId),
                temperatureC = forecast.temp ?: current.temperatureC,
                precipitationProbability = forecast.pop.toPercent(),
                windKph = forecast.windSpeed.metersPerSecondToKph(),
                weatherCode = code,
            )
        }
    }

    private fun List<DailyDto>.toDailyForecasts(
        zoneId: ZoneId,
    ): List<DailyForecast> {
        return map { forecast ->
            val providerCode = forecast.weather.primaryProviderCode()
            DailyForecast(
                dateIso = forecast.dt.toIsoDate(zoneId),
                minTemperatureC = forecast.temp?.min ?: 0.0,
                maxTemperatureC = forecast.temp?.max ?: 0.0,
                precipitationProbability = forecast.pop.toPercent(),
                windKph = forecast.windSpeed.metersPerSecondToKph(),
                weatherCode = OpenWeatherCode.normalize(providerCode),
            )
        }
    }

    private fun List<WeatherConditionDto>.primaryProviderCode(fallback: Int = 804): Int =
        firstOrNull()?.id ?: fallback

    private fun Double?.metersPerSecondToKph(): Double = (this ?: 0.0) * 3.6

    private fun Double?.toPercent(): Int = (((this ?: 0.0) * 100).roundToInt()).coerceIn(0, 100)

    private fun Long?.toIsoDateTime(zoneId: ZoneId): String {
        val instant = Instant.ofEpochSecond(this ?: 0L)
        return instant.atZone(zoneId).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    }

    private fun Long?.toIsoDate(zoneId: ZoneId): String {
        val instant = Instant.ofEpochSecond(this ?: 0L)
        return instant.atZone(zoneId).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun String?.zoneIdOrUtc(): ZoneId = runCatching {
        ZoneId.of(this ?: "UTC")
    }.getOrDefault(ZoneId.of("UTC"))

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
