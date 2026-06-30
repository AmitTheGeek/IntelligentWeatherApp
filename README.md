# Advanced Weather Intelligence App

Native Android weather intelligence app built for the assignment brief. It provides current conditions, hourly and weekly forecasts, offline-first cached access, smart refresh, dynamic weather styling, periodic background sync, and locally derived severe-weather notifications.

## Features

- Current weather for any searched city.
- 24-hour forecast with a Compose temperature chart and precipitation bars.
- 7-day outlook with min/max temperatures and rain probability.
- Offline-first cache backed by a local SQLite database.
- Cache-per-city rows with last-updated timestamps.
- Smart refresh using a 30-minute TTL.
- Retrofit networking against the public Open-Meteo APIs.
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
- `data/remote/` contains Retrofit services and Open-Meteo DTOs.
- `domain/` contains app models, weather-code mapping, refresh policy, and severe-weather detection.
- `sync/WeatherSyncWorker.kt` performs periodic background refresh through WorkManager.
- `notifications/WeatherNotifier.kt` owns notification channel creation and alert display.

Flow direction:

1. UI observes `WeatherViewModel.uiState`.
2. ViewModel observes cached city data from the repository.
3. Repository emits database rows first and refreshes from network only when forced or stale.
4. Successful API responses are written back into SQLite.
5. SQLite updates are emitted through Flow and redraw the Compose UI.

## API

The app uses Open-Meteo because it is public and does not require an API key:

- Geocoding: `https://geocoding-api.open-meteo.com/v1/search`
- Forecast: `https://api.open-meteo.com/v1/forecast`

## Setup

1. Open the project in Android Studio.
2. Use JDK 17 or Android Studio's bundled JBR.
3. Sync Gradle.
4. Run the `app` configuration on an emulator or device.

No API key is required.

## Build And Test

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" ./gradlew testDebugUnitTest assembleDebug
```

The debug APK is generated at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Assumptions

- Open-Meteo does not provide official weather-alert bulletins in the free no-key forecast endpoint, so the app derives local severe-weather signals from wind, gusts, precipitation probability, and thunderstorm weather codes.
- The cache TTL is 30 minutes, which balances freshness and network usage for an assignment app.
- Searched city names are normalized and used as cache keys. The resolved city metadata and coordinates are stored with the cached forecast.
- Background sync requires network connectivity and refreshes only tracked cached cities.
- Android 13+ notification permission is requested on launch so severe-weather notifications can be shown.
