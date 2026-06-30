package com.example.weatherintelligence.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

interface GeocodingService {
    @GET("v1/search")
    suspend fun searchCity(
        @Query("name") name: String,
        @Query("count") count: Int = 1,
        @Query("language") language: String = "en",
        @Query("format") format: String = "json",
    ): GeocodingResponse
}

interface ForecastService {
    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current") current: String = CURRENT_FIELDS,
        @Query("hourly") hourly: String = HOURLY_FIELDS,
        @Query("daily") daily: String = DAILY_FIELDS,
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7,
    ): ForecastResponse

    companion object {
        private const val CURRENT_FIELDS =
            "temperature_2m,relative_humidity_2m,apparent_temperature,weather_code,wind_speed_10m,wind_gusts_10m"
        private const val HOURLY_FIELDS =
            "temperature_2m,precipitation_probability,weather_code,wind_speed_10m"
        private const val DAILY_FIELDS =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max,wind_speed_10m_max"
    }
}

data class GeocodingResponse(
    val results: List<GeocodingResult> = emptyList(),
)

data class GeocodingResult(
    val name: String,
    val country: String? = null,
    val latitude: Double,
    val longitude: Double,
    val timezone: String? = null,
    @SerializedName("admin1") val adminArea: String? = null,
)

data class ForecastResponse(
    val timezone: String? = null,
    val current: CurrentDto? = null,
    val hourly: HourlyDto? = null,
    val daily: DailyDto? = null,
)

data class CurrentDto(
    val time: String? = null,
    @SerializedName("temperature_2m") val temperature: Double? = null,
    @SerializedName("relative_humidity_2m") val humidity: Int? = null,
    @SerializedName("apparent_temperature") val apparentTemperature: Double? = null,
    @SerializedName("weather_code") val weatherCode: Int? = null,
    @SerializedName("wind_speed_10m") val windSpeed: Double? = null,
    @SerializedName("wind_gusts_10m") val windGusts: Double? = null,
)

data class HourlyDto(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m") val temperature: List<Double?> = emptyList(),
    @SerializedName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList(),
    @SerializedName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerializedName("wind_speed_10m") val windSpeed: List<Double?> = emptyList(),
)

data class DailyDto(
    val time: List<String> = emptyList(),
    @SerializedName("temperature_2m_max") val temperatureMax: List<Double?> = emptyList(),
    @SerializedName("temperature_2m_min") val temperatureMin: List<Double?> = emptyList(),
    @SerializedName("precipitation_probability_max") val precipitationProbability: List<Int?> = emptyList(),
    @SerializedName("weather_code") val weatherCode: List<Int?> = emptyList(),
    @SerializedName("wind_speed_10m_max") val windSpeed: List<Double?> = emptyList(),
)
