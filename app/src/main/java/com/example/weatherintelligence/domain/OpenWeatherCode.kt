package com.example.weatherintelligence.domain

import java.util.Locale

object OpenWeatherCode {
    fun normalize(code: Int): Int = when (code) {
        in 200..202, in 230..232 -> 95
        in 210..221 -> 96
        in 300..321 -> 53
        in 500..504 -> 61
        511 -> 66
        in 520..531 -> 80
        in 600..622 -> 71
        in 701..781 -> 45
        800 -> 0
        801 -> 1
        802 -> 2
        803, 804 -> 3
        else -> 3
    }

    fun labelFor(code: Int, description: String?): String {
        return description
            ?.replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
            ?: WeatherCode.labelFor(normalize(code))
    }
}
