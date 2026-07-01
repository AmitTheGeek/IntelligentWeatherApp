# Advanced Weather Intelligence App

Native Android weather intelligence app built for the assignment brief. It provides current conditions, hourly and weekly forecasts, offline-first cached access, smart refresh, dynamic weather styling, periodic background sync, and locally derived severe-weather notifications.

## Features

- Current weather for any searched city.
- 5-day / 3-hour forecast with a Compose temperature chart and precipitation bars.
- Daily outlook aggregated from free forecast windows with min/max temperatures and rain probability.
- Offline-first cache backed by a local SQLite database.
- Cache-per-city rows with last-updated timestamps.
- Smart refresh using a 30-minute TTL.
- Retrofit networking against OpenWeather APIs.
- Coroutines and Flow for database observation plus API refresh.
- WorkManager periodic sync for cached cities.
- Severe weather notification support for storms, heavy rain, and high winds.
- Dynamic Compose theme based on weather condition.
- Unit tests for TTL freshness, weather-code mapping, and alert classification.

## Architecture

The project uses a compact MVVM + Repository structure:

- `ui/` contains Jetpack Compose screens and `WeatherViewModel`.
- `data/repository/WeatherRepository.kt` coordinates cache, TTL, geocoding, forecast refresh, and offline fallback.
- `data/local/` contains a direct SQLite cache implementation used as the source of truth.
- `data/remote/` contains Retrofit services and OpenWeather DTOs.
- `domain/` contains app models, weather-code mapping, refresh policy, and severe-weather detection.
- `sync/WeatherSyncWorker.kt` performs periodic background refresh through WorkManager.
- `notifications/WeatherNotifier.kt` owns notification channel creation and alert display.

Flow direction:

1. UI observes `WeatherViewModel.uiState`.
2. ViewModel observes cached city data from the repository.
3. Repository emits database rows first and refreshes from network only when forced or stale.
4. Successful API responses are written back into SQLite.
5. SQLite updates are emitted through Flow and redraw the Compose UI.

## API Key

The app uses OpenWeather and expects an API key at build time. Keep the key in `local.properties`, which is ignored by Git:

```properties
WEATHER_API_KEY=your_openweather_api_key
```

Do not commit API keys, paste them into README files, or log them from the app. Gradle reads the value from `local.properties`, or from a `WEATHER_API_KEY` environment variable when the local property is absent, and exposes it to the app as `BuildConfig.WEATHER_API_KEY`.

Endpoints used:

- Geocoding: `https://api.openweathermap.org/geo/1.0/direct`
- Current weather: `https://api.openweathermap.org/data/2.5/weather`
- Forecast: `https://api.openweathermap.org/data/2.5/forecast`

## Setup

1. Open the project in Android Studio.
2. Use JDK 17 or Android Studio's bundled JBR.
3. Add `WEATHER_API_KEY` to `local.properties`, or export it as an environment variable before building.
4. Sync Gradle.
5. Run the `app` configuration on an emulator or device.

## Build And Test

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Debugging API Calls

Debug builds log OpenWeather request and response metadata to Logcat with the tag `WeatherApi`. The logger redacts secret query/body fields such as `appid`, `api_key`, `key`, and `token` before writing logs. For failed HTTP responses, it also logs the sanitized error body to help diagnose issues such as `401 Unauthorized`.

## Assumptions

- The app uses OpenWeather's free current weather and 5-day / 3-hour forecast endpoints. Daily forecast cards are aggregated locally from 3-hour forecast windows.
- The app derives local severe-weather signals from wind, gusts, precipitation probability, and thunderstorm weather codes rather than relying on provider alert bulletins.
- The cache TTL is 30 minutes, which balances freshness and network usage for an assignment app.
- Searched city names are normalized and used as cache keys. The resolved city metadata and coordinates are stored with the cached forecast.
- Background sync requires network connectivity and refreshes only tracked cached cities.
- Android 13+ notification permission is requested on launch so severe-weather notifications can be shown.
