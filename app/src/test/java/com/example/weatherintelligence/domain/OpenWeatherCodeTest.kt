package com.example.weatherintelligence.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenWeatherCodeTest {
    @Test
    fun `maps thunderstorm ids to storm code`() {
        assertEquals(95, OpenWeatherCode.normalize(201))
        assertEquals(96, OpenWeatherCode.normalize(211))
    }

    @Test
    fun `maps clear and cloudy ids to expected weather codes`() {
        assertEquals(0, OpenWeatherCode.normalize(800))
        assertEquals(2, OpenWeatherCode.normalize(802))
        assertEquals(3, OpenWeatherCode.normalize(804))
    }

    @Test
    fun `uses provider description as display label`() {
        assertEquals("Light rain", OpenWeatherCode.labelFor(500, "light rain"))
    }
}
