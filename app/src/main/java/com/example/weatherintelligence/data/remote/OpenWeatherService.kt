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
    @GET("data/2.5/weather")
    suspend fun currentWeather(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
        @Query("units") units: String = "metric",
        @Query("appid") apiKey: String,
    ): CurrentWeatherResponse

    @GET("data/2.5/forecast")
    suspend fun forecast(
        @Query("lat") latitude: Double,
        @Query("lon") longitude: Double,
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

data class CurrentWeatherResponse(
    val coord: CoordinateDto? = null,
    val weather: List<WeatherConditionDto> = emptyList(),
    val main: TemperatureDto? = null,
    val wind: WindDto? = null,
    val dt: Long? = null,
    val timezone: Int? = null,
    val name: String? = null,
    val sys: SystemDto? = null,
)

data class ForecastResponse(
    val list: List<ForecastItemDto> = emptyList(),
    val city: ForecastCityDto? = null,
)

data class ForecastItemDto(
    val dt: Long? = null,
    val main: TemperatureDto? = null,
    val wind: WindDto? = null,
    val pop: Double? = null,
    val weather: List<WeatherConditionDto> = emptyList(),
    @SerializedName("dt_txt") val dateText: String? = null,
)

data class ForecastCityDto(
    val name: String? = null,
    val country: String? = null,
    val coord: CoordinateDto? = null,
    val timezone: Int? = null,
)

data class CoordinateDto(
    val lat: Double? = null,
    val lon: Double? = null,
)

data class TemperatureDto(
    val temp: Double? = null,
    @SerializedName("feels_like") val feelsLike: Double? = null,
    @SerializedName("temp_min") val tempMin: Double? = null,
    @SerializedName("temp_max") val tempMax: Double? = null,
    val humidity: Int? = null,
)

data class WindDto(
    val speed: Double? = null,
    val gust: Double? = null,
)

data class SystemDto(
    val country: String? = null,
)

data class WeatherConditionDto(
    val id: Int? = null,
    val main: String? = null,
    val description: String? = null,
)
