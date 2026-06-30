package com.example.weatherintelligence.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherCodeTest {
    @Test
    fun `maps open meteo codes to readable labels`() {
        assertEquals("Clear sky", WeatherCode.labelFor(0))
        assertEquals("Rain showers", WeatherCode.labelFor(81))
        assertEquals("Thunderstorm with hail", WeatherCode.labelFor(99))
    }

    @Test
    fun `maps storm codes to storm mood`() {
        assertEquals(WeatherMood.STORM, WeatherCode.moodFor(95))
        assertTrue(WeatherCode.isStorm(99))
    }
}
