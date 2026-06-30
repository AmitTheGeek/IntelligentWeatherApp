package com.example.weatherintelligence.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class WeatherCacheDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_WEATHER_CACHE (
                $COL_QUERY_KEY TEXT PRIMARY KEY NOT NULL,
                $COL_CITY_NAME TEXT NOT NULL,
                $COL_COUNTRY TEXT NOT NULL,
                $COL_LATITUDE REAL NOT NULL,
                $COL_LONGITUDE REAL NOT NULL,
                $COL_TIMEZONE TEXT NOT NULL,
                $COL_TEMPERATURE REAL NOT NULL,
                $COL_FEELS_LIKE REAL NOT NULL,
                $COL_HUMIDITY INTEGER NOT NULL,
                $COL_WIND REAL NOT NULL,
                $COL_GUST REAL NOT NULL,
                $COL_WEATHER_CODE INTEGER NOT NULL,
                $COL_CONDITION TEXT NOT NULL,
                $COL_HOURLY_JSON TEXT NOT NULL,
                $COL_DAILY_JSON TEXT NOT NULL,
                $COL_UPDATED_AT INTEGER NOT NULL
            )
            """.trimIndent(),
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEATHER_CACHE")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "weather_cache.db"
        private const val DATABASE_VERSION = 1

        const val TABLE_WEATHER_CACHE = "weather_cache"
        const val COL_QUERY_KEY = "query_key"
        const val COL_CITY_NAME = "city_name"
        const val COL_COUNTRY = "country"
        const val COL_LATITUDE = "latitude"
        const val COL_LONGITUDE = "longitude"
        const val COL_TIMEZONE = "timezone"
        const val COL_TEMPERATURE = "temperature"
        const val COL_FEELS_LIKE = "feels_like"
        const val COL_HUMIDITY = "humidity"
        const val COL_WIND = "wind"
        const val COL_GUST = "gust"
        const val COL_WEATHER_CODE = "weather_code"
        const val COL_CONDITION = "condition"
        const val COL_HOURLY_JSON = "hourly_json"
        const val COL_DAILY_JSON = "daily_json"
        const val COL_UPDATED_AT = "updated_at"
    }
}
