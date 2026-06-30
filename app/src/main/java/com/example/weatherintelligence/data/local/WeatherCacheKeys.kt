package com.example.weatherintelligence.data.local

object WeatherCacheKeys {
    fun fromQuery(query: String): String = query
        .trim()
        .lowercase()
        .replace(Regex("\\s+"), " ")
}
