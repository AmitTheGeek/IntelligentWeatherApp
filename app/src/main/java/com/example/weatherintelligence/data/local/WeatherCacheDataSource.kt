package com.example.weatherintelligence.data.local

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_CITY_NAME
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_CONDITION
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_COUNTRY
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_DAILY_JSON
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_FEELS_LIKE
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_GUST
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_HOURLY_JSON
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_HUMIDITY
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_LATITUDE
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_LONGITUDE
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_QUERY_KEY
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_TEMPERATURE
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_TIMEZONE
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_UPDATED_AT
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_WEATHER_CODE
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.COL_WIND
import com.example.weatherintelligence.data.local.WeatherCacheDatabase.Companion.TABLE_WEATHER_CACHE
import com.example.weatherintelligence.domain.CachedCity
import com.example.weatherintelligence.domain.CurrentWeather
import com.example.weatherintelligence.domain.DailyForecast
import com.example.weatherintelligence.domain.HourlyForecast
import com.example.weatherintelligence.domain.WeatherSnapshot
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext

class WeatherCacheDataSource(
    private val database: WeatherCacheDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val gson: Gson = Gson(),
) {
    private val updates = MutableSharedFlow<String>(extraBufferCapacity = 64)
    private val hourlyListType = object : TypeToken<List<HourlyForecast>>() {}.type
    private val dailyListType = object : TypeToken<List<DailyForecast>>() {}.type

    fun observeWeather(queryKey: String): Flow<WeatherSnapshot?> = updates
        .onStart { emit(queryKey) }
        .filter { it == queryKey }
        .map { getWeather(queryKey) }
        .distinctUntilChanged()

    suspend fun getWeather(queryKey: String): WeatherSnapshot? = withContext(ioDispatcher) {
        database.readableDatabase.query(
            TABLE_WEATHER_CACHE,
            null,
            "$COL_QUERY_KEY = ?",
            arrayOf(queryKey),
            null,
            null,
            null,
            "1",
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.toWeatherSnapshot() else null
        }
    }

    suspend fun getTrackedCities(): List<CachedCity> = withContext(ioDispatcher) {
        database.readableDatabase.query(
            TABLE_WEATHER_CACHE,
            arrayOf(
                COL_QUERY_KEY,
                COL_CITY_NAME,
                COL_COUNTRY,
                COL_LATITUDE,
                COL_LONGITUDE,
                COL_TIMEZONE,
                COL_UPDATED_AT,
            ),
            null,
            null,
            null,
            null,
            "$COL_UPDATED_AT DESC",
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(
                        CachedCity(
                            queryKey = cursor.string(COL_QUERY_KEY),
                            cityName = cursor.string(COL_CITY_NAME),
                            country = cursor.string(COL_COUNTRY),
                            latitude = cursor.double(COL_LATITUDE),
                            longitude = cursor.double(COL_LONGITUDE),
                            timezone = cursor.string(COL_TIMEZONE),
                            updatedAtEpochMillis = cursor.long(COL_UPDATED_AT),
                        ),
                    )
                }
            }
        }
    }

    suspend fun upsertWeather(snapshot: WeatherSnapshot) {
        withContext(ioDispatcher) {
            val values = ContentValues().apply {
                put(COL_QUERY_KEY, snapshot.queryKey)
                put(COL_CITY_NAME, snapshot.cityName)
                put(COL_COUNTRY, snapshot.country)
                put(COL_LATITUDE, snapshot.latitude)
                put(COL_LONGITUDE, snapshot.longitude)
                put(COL_TIMEZONE, snapshot.timezone)
                put(COL_TEMPERATURE, snapshot.current.temperatureC)
                put(COL_FEELS_LIKE, snapshot.current.feelsLikeC)
                put(COL_HUMIDITY, snapshot.current.humidityPercent)
                put(COL_WIND, snapshot.current.windKph)
                put(COL_GUST, snapshot.current.gustKph)
                put(COL_WEATHER_CODE, snapshot.current.weatherCode)
                put(COL_CONDITION, snapshot.current.condition)
                put(COL_HOURLY_JSON, gson.toJson(snapshot.hourly))
                put(COL_DAILY_JSON, gson.toJson(snapshot.daily))
                put(COL_UPDATED_AT, snapshot.updatedAtEpochMillis)
            }
            database.writableDatabase.insertWithOnConflict(
                TABLE_WEATHER_CACHE,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE,
            )
        }
        updates.tryEmit(snapshot.queryKey)
    }

    private fun Cursor.toWeatherSnapshot(): WeatherSnapshot {
        val weatherCode = int(COL_WEATHER_CODE)
        return WeatherSnapshot(
            queryKey = string(COL_QUERY_KEY),
            cityName = string(COL_CITY_NAME),
            country = string(COL_COUNTRY),
            latitude = double(COL_LATITUDE),
            longitude = double(COL_LONGITUDE),
            timezone = string(COL_TIMEZONE),
            current = CurrentWeather(
                temperatureC = double(COL_TEMPERATURE),
                feelsLikeC = double(COL_FEELS_LIKE),
                humidityPercent = int(COL_HUMIDITY),
                windKph = double(COL_WIND),
                gustKph = double(COL_GUST),
                weatherCode = weatherCode,
                condition = string(COL_CONDITION),
            ),
            hourly = gson.fromJson(string(COL_HOURLY_JSON), hourlyListType) ?: emptyList(),
            daily = gson.fromJson(string(COL_DAILY_JSON), dailyListType) ?: emptyList(),
            updatedAtEpochMillis = long(COL_UPDATED_AT),
        )
    }

    private fun Cursor.string(column: String): String = getString(getColumnIndexOrThrow(column))

    private fun Cursor.int(column: String): Int = getInt(getColumnIndexOrThrow(column))

    private fun Cursor.long(column: String): Long = getLong(getColumnIndexOrThrow(column))

    private fun Cursor.double(column: String): Double = getDouble(getColumnIndexOrThrow(column))
}
