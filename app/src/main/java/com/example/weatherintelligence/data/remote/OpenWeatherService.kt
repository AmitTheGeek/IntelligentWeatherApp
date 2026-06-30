package com.example.weatherintelligence.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    @GET("geo/1.0/direct")
    suspend fun searchCity(
        @Query("q") query: String,
        @Query("limit") limit: Int = 1,
        @Query("appid") apiKey: String,
    ): List<GeocodingResult>
}

interface ForecastService {
    @GET("data/3.0/onecall")
    suspend fun forecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("exclude") exclude: String = "minutely",
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String,
    ): ForecastResponse
}

data class GeocodingResult(
    val name: String,
    val country: String? = null,
    val lat: Double,
    val lon: Double,
    val state: String? = null,
)

data class ForecastResponse(
    val lat: Double? = null,
    val lon: Double? = null,
    val timezone: String? = null,
    val current: CurrentDto? = null,
    val hourly: List<HourlyDto> = emptyList(),
    val daily: List<DailyDto> = emptyList(),
)

data class CurrentDto(
    val dt: Long? = null,
    val temp: Double? = null,
    @SerializedName("feels_like") val feelsLike: Double? = null,
    val humidity: Int? = null,
    @SerializedName("wind_speed") val windSpeed: Double? = null,
    @SerializedName("wind_gust") val windGust: Double? = null,
    val weather: List<WeatherConditionDto> = emptyList(),
)

data class HourlyDto(
    val dt: Long? = null,
    val temp: Double? = null,
    val pop: Double? = null,
    @SerializedName("wind_speed") val windSpeed: Double? = null,
    val weather: List<WeatherConditionDto> = emptyList(),
)

data class DailyDto(
    val dt: Long? = null,
    val temp: DailyTemperatureDto? = null,
    val pop: Double? = null,
    @SerializedName("wind_speed") val windSpeed: Double? = null,
    val weather: List<WeatherConditionDto> = emptyList(),
)

data class DailyTemperatureDto(
    val min: Double? = null,
    val max: Double? = null,
)

data class WeatherConditionDto(
    val id: Int? = null,
    val main: String? = null,
    val description: String? = null,
)
