package com.example.weatherintelligence.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.weatherintelligence.domain.WeatherMood

data class WeatherPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val accent: Color,
    val accentSoft: Color,
    val card: Color,
    val onCard: Color,
)

@Composable
fun WeatherIntelligenceTheme(
    mood: WeatherMood,
    content: @Composable () -> Unit,
) {
    val palette = paletteFor(mood)
    val scheme = lightColorScheme(
        primary = palette.accent,
        secondary = palette.accentSoft,
        background = palette.backgroundBottom,
        surface = palette.card,
        onSurface = palette.onCard,
        onPrimary = Color.White,
    )

    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}

fun paletteFor(mood: WeatherMood): WeatherPalette = when (mood) {
    WeatherMood.CLEAR -> WeatherPalette(
        backgroundTop = Color(0xFF0EA5E9),
        backgroundBottom = Color(0xFFF8D66D),
        accent = Color(0xFFF97316),
        accentSoft = Color(0xFF7DD3FC),
        card = Color(0xEFFFFFFF),
        onCard = Color(0xFF111827),
    )

    WeatherMood.CLOUDY -> WeatherPalette(
        backgroundTop = Color(0xFF64748B),
        backgroundBottom = Color(0xFFCBD5E1),
        accent = Color(0xFF2563EB),
        accentSoft = Color(0xFF94A3B8),
        card = Color(0xF5FFFFFF),
        onCard = Color(0xFF0F172A),
    )

    WeatherMood.RAIN -> WeatherPalette(
        backgroundTop = Color(0xFF155E75),
        backgroundBottom = Color(0xFF67E8F9),
        accent = Color(0xFF0891B2),
        accentSoft = Color(0xFFA7F3D0),
        card = Color(0xF2F8FAFC),
        onCard = Color(0xFF0C2431),
    )

    WeatherMood.SNOW -> WeatherPalette(
        backgroundTop = Color(0xFF93C5FD),
        backgroundBottom = Color(0xFFE0F2FE),
        accent = Color(0xFF2563EB),
        accentSoft = Color(0xFFBAE6FD),
        card = Color(0xF8FFFFFF),
        onCard = Color(0xFF0F172A),
    )

    WeatherMood.STORM -> WeatherPalette(
        backgroundTop = Color(0xFF312E81),
        backgroundBottom = Color(0xFF475569),
        accent = Color(0xFFEAB308),
        accentSoft = Color(0xFF818CF8),
        card = Color(0xF1FFFFFF),
        onCard = Color(0xFF111827),
    )

    WeatherMood.FOG -> WeatherPalette(
        backgroundTop = Color(0xFF94A3B8),
        backgroundBottom = Color(0xFFE2E8F0),
        accent = Color(0xFF0F766E),
        accentSoft = Color(0xFFCBD5E1),
        card = Color(0xF7FFFFFF),
        onCard = Color(0xFF172033),
    )
}
