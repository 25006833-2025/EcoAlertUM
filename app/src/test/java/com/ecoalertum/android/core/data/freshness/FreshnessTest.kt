package com.ecoalertum.android.core.data.freshness

import com.ecoalertum.android.core.data.TestData.MINUTE
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.model.SourceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshnessTest {

    @Test
    fun dataWithinMaxAgeIsNotOutdated() {
        assertFalse(Freshness.isOutdated(NOW - 10 * MINUTE, maxAgeMillis = 15 * MINUTE, nowMillis = NOW))
    }

    @Test
    fun dataExactlyAtMaxAgeIsNotOutdated() {
        assertFalse(Freshness.isOutdated(NOW - 15 * MINUTE, maxAgeMillis = 15 * MINUTE, nowMillis = NOW))
    }

    @Test
    fun dataOlderThanMaxAgeIsOutdated() {
        assertTrue(Freshness.isOutdated(NOW - 15 * MINUTE - 1, maxAgeMillis = 15 * MINUTE, nowMillis = NOW))
    }

    @Test
    fun unknownUpdateTimeIsOutdated() {
        assertTrue(Freshness.isOutdated(null, maxAgeMillis = 15 * MINUTE, nowMillis = NOW))
    }

    @Test
    fun timestampInTheFutureIsNotOutdated() {
        assertFalse(Freshness.isOutdated(NOW + 5 * MINUTE, maxAgeMillis = 15 * MINUTE, nowMillis = NOW))
    }

    @Test
    fun lastUpdatedUsesLaterOfFetchedAtAndSourceSuccess() {
        val reading = HazardReading(source = Source.INFOBANJIR, fetchedAt = NOW - 90 * MINUTE)
        val status = SourceStatus(Source.INFOBANJIR, lastSuccessAt = NOW - 5 * MINUTE)

        assertEquals(NOW - 5 * MINUTE, Freshness.lastUpdatedOf(reading, status))
        assertEquals(NOW - 90 * MINUTE, Freshness.lastUpdatedOf(reading, null))
        assertNull(Freshness.lastUpdatedOf(HazardReading(), null))
    }

    @Test
    fun defaultPolicyAllowsThreeMissedFetchesPerSource() {
        val policy = FreshnessPolicy()
        assertEquals(15 * MINUTE, policy.maxAgeFor(Source.MET))
        assertEquals(45 * MINUTE, policy.maxAgeFor(Source.INFOBANJIR))
        assertEquals(45 * MINUTE, policy.maxAgeFor(Source.DOE))

        assertTrue(policy.isOutdated(Source.MET, NOW - 20 * MINUTE, NOW))
        assertFalse(policy.isOutdated(Source.DOE, NOW - 20 * MINUTE, NOW))
    }

    @Test
    fun customPolicyOverridesOneSourceOnly() {
        val policy = FreshnessPolicy(mapOf(Source.MET to 60 * MINUTE))
        assertEquals(60 * MINUTE, policy.maxAgeFor(Source.MET))
        assertEquals(45 * MINUTE, policy.maxAgeFor(Source.DOE))
    }
}
