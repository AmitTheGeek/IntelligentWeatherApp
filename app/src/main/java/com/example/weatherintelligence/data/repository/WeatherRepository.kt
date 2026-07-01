package com.example.weatherintelligence.data.repository

import com.example.weatherintelligence.data.local.WeatherCacheDataSource
import com.example.weatherintelligence.data.local.WeatherCacheKeys
import com.example.weatherintelligence.data.remote.CurrentWeatherResponse
import com.example.weatherintelligence.data.remote.ForecastResponse
import com.example.weatherintelligence.data.remote.ForecastService
import com.example.weatherintelligence.data.remote.ForecastItemDto
import com.example.weatherintelligence.data.remote.GeocodingResult
import com.example.weatherintelligence.data.remote.GeocodingService
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
import java.time.ZoneOffset
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
            val key = requireApiKey()
            val currentResponse = forecastService.currentWeather(
                latitude = location.latitude,
                longitude = location.longitude,
                apiKey = key,
            )
            val forecastResponse = forecastService.forecast(
                latitude = location.latitude,
                longitude = location.longitude,
                apiKey = key,
            )
            val snapshot = toSnapshot(
                queryKey = queryKey,
                location = location,
                currentResponse = currentResponse,
                forecastResponse = forecastResponse,
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

    private fun toSnapshot(
        queryKey: String,
        location: WeatherLocation,
        currentResponse: CurrentWeatherResponse,
        forecastResponse: ForecastResponse,
        nowEpochMillis: Long,
    ): WeatherSnapshot {
        val currentMain = currentResponse.main
            ?: forecastResponse.list.firstOrNull()?.main
            ?: throw IllegalStateException("Weather response is missing temperature conditions.")
        val zoneId = (forecastResponse.city?.timezone ?: currentResponse.timezone).zoneOffsetOrUtc()
        val providerCode = currentResponse.weather.primaryProviderCode(
            forecastResponse.list.firstOrNull()?.weather.primaryProviderCode(),
        )
        val code = OpenWeatherCode.normalize(providerCode)
        val currentWeather = CurrentWeather(
            temperatureC = currentMain.temp ?: 0.0,
            feelsLikeC = currentMain.feelsLike ?: currentMain.temp ?: 0.0,
            humidityPercent = currentMain.humidity ?: 0,
            windKph = currentResponse.wind?.speed.metersPerSecondToKph(),
            gustKph = currentResponse.wind?.gust.metersPerSecondToKph().takeIf { it > 0.0 }
                ?: currentResponse.wind?.speed.metersPerSecondToKph(),
            weatherCode = code,
            condition = OpenWeatherCode.labelFor(providerCode, currentResponse.weather.firstOrNull()?.description),
        )

        return WeatherSnapshot(
            queryKey = queryKey,
            cityName = currentResponse.name ?: forecastResponse.city?.name ?: location.cityName,
            country = location.country,
            latitude = currentResponse.coord?.lat ?: forecastResponse.city?.coord?.lat ?: location.latitude,
            longitude = currentResponse.coord?.lon ?: forecastResponse.city?.coord?.lon ?: location.longitude,
            timezone = zoneId.id,
            current = currentWeather,
            hourly = forecastResponse.list.toHourlyForecasts(currentWeather, zoneId).take(24),
            daily = forecastResponse.list.toDailyForecasts(zoneId),
            updatedAtEpochMillis = nowEpochMillis,
        )
    }

    private fun List<ForecastItemDto>.toHourlyForecasts(
        current: CurrentWeather,
        zoneId: ZoneId,
    ): List<HourlyForecast> {
        return map { forecast ->
            val providerCode = forecast.weather.firstOrNull()?.id
            val code = providerCode?.let(OpenWeatherCode::normalize) ?: current.weatherCode
            HourlyForecast(
                timeIso = forecast.dt.toIsoDateTime(zoneId),
                temperatureC = forecast.main?.temp ?: current.temperatureC,
                precipitationProbability = forecast.pop.toPercent(),
                windKph = forecast.wind?.speed.metersPerSecondToKph(),
                weatherCode = code,
            )
        }
    }

    private fun List<ForecastItemDto>.toDailyForecasts(
        zoneId: ZoneId,
    ): List<DailyForecast> {
        return groupBy { forecast -> forecast.dt.toIsoDate(zoneId) }
            .toSortedMap()
            .map { (dateIso, forecasts) ->
                val temperatures = forecasts.mapNotNull { it.main?.temp }
                val minTemperatures = forecasts.mapNotNull { it.main?.tempMin ?: it.main?.temp }
                val maxTemperatures = forecasts.mapNotNull { it.main?.tempMax ?: it.main?.temp }
                val mostRelevantForecast = forecasts.maxWithOrNull(
                    compareBy<ForecastItemDto> { it.pop.toPercent() }
                        .thenBy { it.wind?.speed.metersPerSecondToKph() },
                ) ?: forecasts.first()
                val providerCode = mostRelevantForecast.weather.primaryProviderCode()

                DailyForecast(
                    dateIso = dateIso,
                    minTemperatureC = minTemperatures.minOrNull() ?: temperatures.minOrNull() ?: 0.0,
                    maxTemperatureC = maxTemperatures.maxOrNull() ?: temperatures.maxOrNull() ?: 0.0,
                    precipitationProbability = forecasts.maxOfOrNull { it.pop.toPercent() } ?: 0,
                    windKph = forecasts.maxOfOrNull { it.wind?.speed.metersPerSecondToKph() } ?: 0.0,
                    weatherCode = OpenWeatherCode.normalize(providerCode),
                )
            }
            .take(7)
    }

    private fun List<WeatherConditionDto>?.primaryProviderCode(fallback: Int = 804): Int =
        this?.firstOrNull()?.id ?: fallback

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

    private fun Int?.zoneOffsetOrUtc(): ZoneId = ZoneOffset.ofTotalSeconds(this ?: 0)

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
