package com.example.weatherintelligence.data.remote

import android.util.Log
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody

class SafeNetworkLoggingInterceptor(
    private val enabled: Boolean,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        if (!enabled) return chain.proceed(chain.request())

        val request = chain.request()
        val sanitizedUrl = NetworkLogSanitizer.sanitizeUrl(request.url().toString())
        Log.d(TAG, "--> ${request.method()} $sanitizedUrl")

        val startedAt = System.nanoTime()
        val response = try {
            chain.proceed(request)
        } catch (error: IOException) {
            val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
            Log.e(TAG, "<-- HTTP FAILED after ${durationMs}ms $sanitizedUrl: ${error.message}")
            throw error
        }

        val durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt)
        if (!response.shouldLogBody()) {
            Log.d(TAG, "<-- ${response.code()} ${response.message()} (${durationMs}ms) $sanitizedUrl")
            return response
        }

        val body = response.body() ?: return response
        val contentType = body.contentType()
        val rawBody = body.string()
        val safeBody = NetworkLogSanitizer.sanitizeBody(rawBody).take(MAX_BODY_LOG_CHARS)
        Log.d(TAG, "<-- ${response.code()} ${response.message()} (${durationMs}ms) $sanitizedUrl body=$safeBody")

        return response.newBuilder()
            .body(ResponseBody.create(contentType, rawBody))
            .build()
    }

    private fun Response.shouldLogBody(): Boolean {
        return code() >= 400 && body()?.contentType()?.isHumanReadable() == true
    }

    private fun MediaType.isHumanReadable(): Boolean {
        val subtype = subtype().lowercase(Locale.US)
        val type = type().lowercase(Locale.US)
        return type == "text" ||
            subtype.contains("json") ||
            subtype.contains("xml") ||
            subtype.contains("html") ||
            subtype.contains("plain")
    }

    private companion object {
        const val TAG = "WeatherApi"
        const val MAX_BODY_LOG_CHARS = 2_000
    }
}

object NetworkLogSanitizer {
    private val querySecretPattern = Regex("(?i)([?&](?:appid|api_key|apikey|key|token|access_token)=)[^&]*")
    private val jsonSecretPattern = Regex("(?i)(\"(?:appid|api_key|apikey|key|token|access_token)\"\\s*:\\s*\")[^\"]*(\")")

    fun sanitizeUrl(url: String): String =
        url.replace(querySecretPattern) { match -> "${match.groupValues[1]}<redacted>" }

    fun sanitizeBody(body: String): String =
        body.replace(jsonSecretPattern) { match -> "${match.groupValues[1]}<redacted>${match.groupValues[2]}" }
}
