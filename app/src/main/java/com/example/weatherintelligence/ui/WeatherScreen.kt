package com.example.weatherintelligence.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.weatherintelligence.domain.AlertSeverity
import com.example.weatherintelligence.domain.CurrentWeather
import com.example.weatherintelligence.domain.DailyForecast
import com.example.weatherintelligence.domain.HourlyForecast
import com.example.weatherintelligence.domain.WeatherMood
import com.example.weatherintelligence.domain.WeatherSnapshot
import com.example.weatherintelligence.ui.theme.WeatherIntelligenceTheme
import com.example.weatherintelligence.ui.theme.paletteFor
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToInt

@Composable
fun WeatherApp(viewModel: WeatherViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val mood = state.weather?.mood ?: WeatherMood.CLOUDY
    WeatherIntelligenceTheme(mood = mood) {
        WeatherScreen(
            state = state,
            onQueryChanged = viewModel::onQueryChanged,
            onSearch = viewModel::search,
            onRefresh = { viewModel.refresh(force = true) },
        )
    }
}

@Composable
private fun WeatherScreen(
    state: WeatherUiState,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
) {
    val mood = state.weather?.mood ?: WeatherMood.CLOUDY
    val palette = paletteFor(mood)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(palette.backgroundTop, palette.backgroundBottom),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                HeaderSearch(
                    query = state.query,
                    isRefreshing = state.isRefreshing,
                    onQueryChanged = onQueryChanged,
                    onSearch = onSearch,
                    onRefresh = onRefresh,
                    accent = palette.accent,
                    card = palette.card,
                    onCard = palette.onCard,
                )
            }

            state.errorMessage?.let { message ->
                item {
                    MessageStrip(
                        message = message,
                        background = Color(0xFFFFF7ED),
                        foreground = Color(0xFF7C2D12),
                    )
                }
            }

            val weather = state.weather
            if (weather == null) {
                item {
                    EmptyWeatherCard(card = palette.card, onCard = palette.onCard)
                }
            } else {
                item {
                    CurrentWeatherCard(weather = weather, card = palette.card, onCard = palette.onCard)
                }
                item {
                    AlertCard(weather = weather, card = palette.card, accent = palette.accent, onCard = palette.onCard)
                }
                item {
                    MetricGrid(current = weather.current, card = palette.card, onCard = palette.onCard)
                }
                item {
                    HourlyChart(
                        hourly = weather.hourly,
                        card = palette.card,
                        accent = palette.accent,
                        onCard = palette.onCard,
                    )
                }
                item {
                    WeeklyForecast(
                        daily = weather.daily,
                        card = palette.card,
                        onCard = palette.onCard,
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderSearch(
    query: String,
    isRefreshing: Boolean,
    onQueryChanged: (String) -> Unit,
    onSearch: () -> Unit,
    onRefresh: () -> Unit,
    accent: Color,
    card: Color,
    onCard: Color,
) {
    Surface(
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Weather Intelligence",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Offline-first forecasts with smart refresh",
                        style = MaterialTheme.typography.bodyMedium,
                        color = onCard.copy(alpha = 0.72f),
                    )
                }
                if (isRefreshing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accent,
                        strokeWidth = 3.dp,
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChanged,
                    modifier = Modifier.weight(1f),
                    label = { Text("City") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                )
                Button(
                    onClick = onSearch,
                    enabled = !isRefreshing,
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text("Search")
                }
            }

            Button(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = onCard.copy(alpha = 0.88f)),
                shape = RoundedCornerShape(8.dp),
            ) {
                Text("Refresh now")
            }
        }
    }
}

@Composable
private fun EmptyWeatherCard(card: Color, onCard: Color) {
    Surface(
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "No cached forecast yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Search for a city while online. After the first load, the same city remains available offline.",
                style = MaterialTheme.typography.bodyMedium,
                color = onCard.copy(alpha = 0.72f),
            )
        }
    }
}

@Composable
private fun CurrentWeatherCard(weather: WeatherSnapshot, card: Color, onCard: Color) {
    Surface(
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
        tonalElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = weather.cityName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = weather.country,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onCard.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "${weather.current.temperatureC.roundToInt()} C",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = weather.current.condition,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Updated ${weather.updatedAtEpochMillis.formatUpdated()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = onCard.copy(alpha = 0.64f),
                )
            }
            WeatherVisual(
                mood = weather.mood,
                modifier = Modifier.size(108.dp),
            )
        }
    }
}

@Composable
private fun AlertCard(weather: WeatherSnapshot, card: Color, accent: Color, onCard: Color) {
    val alert = weather.alert
    val marker = when (alert.severity) {
        AlertSeverity.NORMAL -> Color(0xFF10B981)
        AlertSeverity.WATCH -> Color(0xFFF59E0B)
        AlertSeverity.SEVERE -> Color(0xFFDC2626)
    }

    Surface(
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(modifier = Modifier.size(18.dp)) {
                drawCircle(marker)
                drawCircle(accent.copy(alpha = 0.28f), radius = size.minDimension / 2f)
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = alert.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = onCard.copy(alpha = 0.72f),
                )
            }
        }
    }
}

@Composable
private fun MetricGrid(current: CurrentWeather, card: Color, onCard: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "Feels like",
                value = "${current.feelsLikeC.roundToInt()} C",
                card = card,
                onCard = onCard,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Humidity",
                value = "${current.humidityPercent}%",
                card = card,
                onCard = onCard,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricCard(
                label = "Wind",
                value = "${current.windKph.roundToInt()} km/h",
                card = card,
                onCard = onCard,
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Gusts",
                value = "${current.gustKph.roundToInt()} km/h",
                card = card,
                onCard = onCard,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    card: Color,
    onCard: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = onCard.copy(alpha = 0.65f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun HourlyChart(
    hourly: List<HourlyForecast>,
    card: Color,
    accent: Color,
    onCard: Color,
) {
    Surface(
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Hourly temperature",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (hourly.isEmpty()) {
                Text("No hourly forecast available.", color = onCard.copy(alpha = 0.7f))
            } else {
                TemperatureLineChart(
                    hourly = hourly.take(12),
                    accent = accent,
                    onCard = onCard,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    hourly.take(4).forEach { point ->
                        Text(
                            text = point.timeIso.formatHour(),
                            style = MaterialTheme.typography.labelSmall,
                            color = onCard.copy(alpha = 0.66f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TemperatureLineChart(
    hourly: List<HourlyForecast>,
    accent: Color,
    onCard: Color,
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val temps = hourly.map { it.temperatureC }
        val minTemp = temps.minOrNull() ?: 0.0
        val maxTemp = temps.maxOrNull() ?: 1.0
        val range = max(1.0, maxTemp - minTemp)
        val startX = 12.dp.toPx()
        val endX = size.width - 12.dp.toPx()
        val topY = 16.dp.toPx()
        val bottomY = size.height - 28.dp.toPx()
        val step = if (hourly.size > 1) (endX - startX) / (hourly.size - 1) else 0f

        repeat(4) { index ->
            val y = topY + (bottomY - topY) * index / 3f
            drawLine(
                color = onCard.copy(alpha = 0.10f),
                start = Offset(startX, y),
                end = Offset(endX, y),
                strokeWidth = 1.dp.toPx(),
            )
        }

        hourly.forEachIndexed { index, point ->
            val barHeight = (point.precipitationProbability.coerceIn(0, 100) / 100f) * 28.dp.toPx()
            val x = startX + step * index
            drawRoundRect(
                color = accent.copy(alpha = 0.22f),
                topLeft = Offset(x - 4.dp.toPx(), bottomY - barHeight),
                size = Size(8.dp.toPx(), barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx()),
            )
        }

        val path = Path()
        hourly.forEachIndexed { index, point ->
            val x = startX + step * index
            val y = bottomY - ((point.temperatureC - minTemp) / range).toFloat() * (bottomY - topY)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = accent,
            style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
        )

        hourly.forEachIndexed { index, point ->
            val x = startX + step * index
            val y = bottomY - ((point.temperatureC - minTemp) / range).toFloat() * (bottomY - topY)
            drawCircle(color = Color.White, radius = 5.dp.toPx(), center = Offset(x, y))
            drawCircle(color = accent, radius = 3.dp.toPx(), center = Offset(x, y))
        }
    }
}

@Composable
private fun WeeklyForecast(daily: List<DailyForecast>, card: Color, onCard: Color) {
    Surface(
        color = card,
        contentColor = onCard,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "7-day outlook",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (daily.isEmpty()) {
                Text("No daily forecast available.", color = onCard.copy(alpha = 0.7f))
            } else {
                daily.take(7).forEach { day ->
                    DailyForecastRow(day = day, onCard = onCard)
                }
            }
        }
    }
}

@Composable
private fun DailyForecastRow(day: DailyForecast, onCard: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = day.dateIso.formatDay(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = "${day.precipitationProbability}% rain chance",
                style = MaterialTheme.typography.labelSmall,
                color = onCard.copy(alpha = 0.62f),
            )
        }
        Text(
            text = "${day.minTemperatureC.roundToInt()} / ${day.maxTemperatureC.roundToInt()} C",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun MessageStrip(message: String, background: Color, foreground: Color) {
    Surface(
        color = background,
        contentColor = foreground,
        shape = RoundedCornerShape(8.dp),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun WeatherVisual(mood: WeatherMood, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width * 0.52f, size.height * 0.48f)
        val cloud = Color.White.copy(alpha = 0.86f)
        val sun = Color(0xFFFFD166)
        val rain = Color(0xFF38BDF8)
        val storm = Color(0xFFFACC15)

        when (mood) {
            WeatherMood.CLEAR -> {
                drawCircle(sun, radius = size.minDimension * 0.28f, center = center)
                repeat(8) { index ->
                    val angle = Math.toRadians(index * 45.0)
                    val start = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.36f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.36f,
                    )
                    val end = Offset(
                        x = center.x + kotlin.math.cos(angle).toFloat() * size.minDimension * 0.46f,
                        y = center.y + kotlin.math.sin(angle).toFloat() * size.minDimension * 0.46f,
                    )
                    drawLine(sun, start, end, strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
                }
            }

            WeatherMood.CLOUDY, WeatherMood.RAIN, WeatherMood.STORM, WeatherMood.SNOW, WeatherMood.FOG -> {
                drawCircle(cloud, radius = size.minDimension * 0.22f, center = Offset(size.width * 0.42f, size.height * 0.46f))
                drawCircle(cloud, radius = size.minDimension * 0.27f, center = Offset(size.width * 0.58f, size.height * 0.40f))
                drawRoundRect(
                    color = cloud,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.47f),
                    size = Size(size.width * 0.62f, size.height * 0.26f),
                    cornerRadius = CornerRadius(28.dp.toPx(), 28.dp.toPx()),
                )
            }
        }

        if (mood == WeatherMood.RAIN) {
            repeat(4) { index ->
                val x = size.width * (0.32f + index * 0.12f)
                drawLine(
                    rain,
                    start = Offset(x, size.height * 0.76f),
                    end = Offset(x - 8.dp.toPx(), size.height * 0.92f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }

        if (mood == WeatherMood.STORM) {
            val bolt = Path().apply {
                moveTo(size.width * 0.54f, size.height * 0.64f)
                lineTo(size.width * 0.44f, size.height * 0.82f)
                lineTo(size.width * 0.56f, size.height * 0.82f)
                lineTo(size.width * 0.48f, size.height * 0.98f)
            }
            drawPath(bolt, storm, style = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round))
        }

        if (mood == WeatherMood.SNOW) {
            repeat(5) { index ->
                drawCircle(
                    Color.White,
                    radius = 3.dp.toPx(),
                    center = Offset(size.width * (0.28f + index * 0.11f), size.height * (0.80f + (index % 2) * 0.09f)),
                )
            }
        }

        if (mood == WeatherMood.FOG) {
            repeat(3) { index ->
                val y = size.height * (0.74f + index * 0.09f)
                drawLine(
                    Color.White.copy(alpha = 0.78f),
                    start = Offset(size.width * 0.22f, y),
                    end = Offset(size.width * 0.82f, y),
                    strokeWidth = 4.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private fun String.formatHour(): String = runCatching {
    LocalDateTime.parse(this).format(DateTimeFormatter.ofPattern("ha", Locale.getDefault()))
}.getOrDefault(takeLast(5))

private fun String.formatDay(): String = runCatching {
    LocalDate.parse(this).format(DateTimeFormatter.ofPattern("EEE, MMM d", Locale.getDefault()))
}.getOrDefault(this)

private fun Long.formatUpdated(): String = runCatching {
    Instant.ofEpochMilli(this)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.getDefault()))
}.getOrDefault("just now")
