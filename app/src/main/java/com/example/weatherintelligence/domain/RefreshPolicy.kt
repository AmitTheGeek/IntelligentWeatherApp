package com.example.weatherintelligence.domain

class RefreshPolicy(
    private val ttlMillis: Long = DEFAULT_TTL_MILLIS,
) {
    fun isFresh(updatedAtEpochMillis: Long, nowEpochMillis: Long = System.currentTimeMillis()): Boolean {
        if (updatedAtEpochMillis <= 0L) return false
        return nowEpochMillis - updatedAtEpochMillis in 0 until ttlMillis
    }

    companion object {
        const val DEFAULT_TTL_MILLIS: Long = 30L * 60L * 1000L
    }
}
