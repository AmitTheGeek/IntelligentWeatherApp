package com.example.weatherintelligence.domain

data class WeatherSnapshot(
    val queryKey: String,
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current: CurrentWeather,
    val hourly: List<HourlyForecast>,
    val daily: List<DailyForecast>,
    val updatedAtEpochMillis: Long,
) {
    val mood: WeatherMood = WeatherCode.moodFor(current.weatherCode)
    val alert: WeatherAlert = SevereWeatherDetector.evaluate(this)
}

data class CurrentWeather(
    val temperatureC: Double,
    val feelsLikeC: Double,
    val humidityPercent: Int,
    val windKph: Double,
    val gustKph: Double,
    val weatherCode: Int,
    val condition: String,
)

data class HourlyForecast(
    val timeIso: String,
    val temperatureC: Double,
    val precipitationProbability: Int,
    val windKph: Double,
    val weatherCode: Int,
)

data class DailyForecast(
    val dateIso: String,
    val minTemperatureC: Double,
    val maxTemperatureC: Double,
    val precipitationProbability: Int,
    val windKph: Double,
    val weatherCode: Int,
)

data class CachedCity(
    val queryKey: String,
    val cityName: String,
    val country: String,
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val updatedAtEpochMillis: Long,
)

enum class WeatherMood {
    CLEAR,
    CLOUDY,
    RAIN,
    SNOW,
    STORM,
    FOG,
}

enum class AlertSeverity {
    NORMAL,
    WATCH,
    SEVERE,
}

data class WeatherAlert(
    val severity: AlertSeverity,
    val title: String,
    val message: String,
)
