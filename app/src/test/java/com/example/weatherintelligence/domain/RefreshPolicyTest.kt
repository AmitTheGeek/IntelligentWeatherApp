package com.example.weatherintelligence.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RefreshPolicyTest {
    @Test
    fun `cache inside ttl is fresh`() {
        val policy = RefreshPolicy(ttlMillis = 1_000L)

        assertTrue(policy.isFresh(updatedAtEpochMillis = 5_000L, nowEpochMillis = 5_500L))
    }

    @Test
    fun `cache at ttl boundary is stale`() {
        val policy = RefreshPolicy(ttlMillis = 1_000L)

        assertFalse(policy.isFresh(updatedAtEpochMillis = 5_000L, nowEpochMillis = 6_000L))
    }

    @Test
    fun `future or missing timestamps are stale`() {
        val policy = RefreshPolicy(ttlMillis = 1_000L)

        assertFalse(policy.isFresh(updatedAtEpochMillis = 0L, nowEpochMillis = 6_000L))
        assertFalse(policy.isFresh(updatedAtEpochMillis = 7_000L, nowEpochMillis = 6_000L))
    }
}
