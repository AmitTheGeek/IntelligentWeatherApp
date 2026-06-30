package com.example.weatherintelligence.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SevereWeatherDetectorTest {
    @Test
    fun `normal weather has no alert`() {
        val alert = SevereWeatherDetector.evaluate(snapshot())

        assertEquals(AlertSeverity.NORMAL, alert.severity)
    }

    @Test
    fun `high gusts produce severe wind alert`() {
        val alert = SevereWeatherDetector.evaluate(snapshot(gustKph = 96.0))

        assertEquals(AlertSeverity.SEVERE, alert.severity)
        assertEquals("Severe wind risk", alert.title)
    }

    @Test
    fun `storm with high precipitation is severe`() {
        val alert = SevereWeatherDetector.evaluate(
            snapshot(
                currentCode = 95,
                dailyPrecipitation = 82,
            ),
        )

        assertEquals(AlertSeverity.SEVERE, alert.severity)
        assertEquals("Severe storm risk", alert.title)
    }

    @Test
    fun `strong wind below severe threshold produces watch`() {
        val alert = SevereWeatherDetector.evaluate(snapshot(windKph = 58.0))

        assertEquals(AlertSeverity.WATCH, alert.severity)
        assertEquals("Wind watch", alert.title)
    }

    private fun snapshot(
        currentCode: Int = 0,
        windKph: Double = 12.0,
        gustKph: Double = 18.0,
        dailyWindKph: Double = 16.0,
        dailyPrecipitation: Int = 20,
    ) = WeatherSnapshot(
        queryKey = "test city",
        cityName = "Test City",
        country = "Test Region",
        latitude = 1.0,
        longitude = 2.0,
        timezone = "UTC",
        current = CurrentWeather(
            temperatureC = 24.0,
            feelsLikeC = 25.0,
            humidityPercent = 55,
            windKph = windKph,
            gustKph = gustKph,
            weatherCode = currentCode,
            condition = WeatherCode.labelFor(currentCode),
        ),
        hourly = listOf(
            HourlyForecast(
                timeIso = "2026-06-30T12:00",
                temperatureC = 24.0,
                precipitationProbability = dailyPrecipitation,
                windKph = windKph,
                weatherCode = currentCode,
            ),
        ),
        daily = listOf(
            DailyForecast(
                dateIso = "2026-06-30",
                minTemperatureC = 20.0,
                maxTemperatureC = 28.0,
                precipitationProbability = dailyPrecipitation,
                windKph = dailyWindKph,
                weatherCode = currentCode,
            ),
        ),
        updatedAtEpochMillis = 1_000L,
    )
}
