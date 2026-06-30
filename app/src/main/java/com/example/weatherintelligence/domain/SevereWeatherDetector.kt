package com.example.weatherintelligence.domain

object SevereWeatherDetector {
    fun evaluate(snapshot: WeatherSnapshot): WeatherAlert {
        val dailyMaxWind = snapshot.daily.maxOfOrNull { it.windKph } ?: 0.0
        val dailyMaxPrecipitation = snapshot.daily.maxOfOrNull { it.precipitationProbability } ?: 0
        val stormExpected = WeatherCode.isStorm(snapshot.current.weatherCode) ||
            snapshot.hourly.take(12).any { WeatherCode.isStorm(it.weatherCode) }

        return when {
            snapshot.current.gustKph >= 90.0 || dailyMaxWind >= 85.0 -> WeatherAlert(
                severity = AlertSeverity.SEVERE,
                title = "Severe wind risk",
                message = "Damaging wind gusts are possible. Avoid exposed travel and monitor local alerts.",
            )

            stormExpected && dailyMaxPrecipitation >= 70 -> WeatherAlert(
                severity = AlertSeverity.SEVERE,
                title = "Severe storm risk",
                message = "Thunderstorms and heavy precipitation are likely in the next 12 hours.",
            )

            snapshot.current.windKph >= 55.0 || dailyMaxWind >= 60.0 -> WeatherAlert(
                severity = AlertSeverity.WATCH,
                title = "Wind watch",
                message = "Strong winds are expected. Secure loose outdoor items.",
            )

            dailyMaxPrecipitation >= 80 -> WeatherAlert(
                severity = AlertSeverity.WATCH,
                title = "Heavy rain watch",
                message = "High precipitation chances may affect commute windows.",
            )

            else -> WeatherAlert(
                severity = AlertSeverity.NORMAL,
                title = "No severe weather",
                message = "No severe weather signals were detected for this forecast window.",
            )
        }
    }
}
