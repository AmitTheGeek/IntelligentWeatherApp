package com.example.weatherintelligence.data.remote

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkLogSanitizerTest {
    @Test
    fun `redacts appid query value from urls`() {
        val sanitized = NetworkLogSanitizer.sanitizeUrl(
            "https://api.openweathermap.org/data/3.0/onecall?lat=12&appid=secret-value&units=metric",
        )

        assertTrue(sanitized.contains("appid=<redacted>"))
        assertFalse(sanitized.contains("secret-value"))
    }

    @Test
    fun `redacts common json secret fields`() {
        val sanitized = NetworkLogSanitizer.sanitizeBody(
            """{"cod":401,"appid":"secret-value","message":"Invalid API key"}""",
        )

        assertTrue(sanitized.contains(""""appid":"<redacted>""""))
        assertFalse(sanitized.contains("secret-value"))
    }
}
