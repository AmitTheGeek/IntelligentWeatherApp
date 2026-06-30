package com.example.weatherintelligence.domain

object WeatherCode {
    fun labelFor(code: Int): String = when (code) {
        0 -> "Clear sky"
        1 -> "Mostly clear"
        2 -> "Partly cloudy"
        3 -> "Overcast"
        45, 48 -> "Fog"
        51, 53, 55 -> "Drizzle"
        56, 57 -> "Freezing drizzle"
        61, 63, 65 -> "Rain"
        66, 67 -> "Freezing rain"
        71, 73, 75 -> "Snow"
        77 -> "Snow grains"
        80, 81, 82 -> "Rain showers"
        85, 86 -> "Snow showers"
        95 -> "Thunderstorm"
        96, 99 -> "Thunderstorm with hail"
        else -> "Changing weather"
    }

    fun moodFor(code: Int): WeatherMood = when (code) {
        0, 1 -> WeatherMood.CLEAR
        2, 3 -> WeatherMood.CLOUDY
        45, 48 -> WeatherMood.FOG
        in 51..67, in 80..82 -> WeatherMood.RAIN
        in 71..77, 85, 86 -> WeatherMood.SNOW
        in 95..99 -> WeatherMood.STORM
        else -> WeatherMood.CLOUDY
    }

    fun isStorm(code: Int): Boolean = code in 95..99
}
