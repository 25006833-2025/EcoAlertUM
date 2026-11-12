package com.ecoalertum.android.core.data.freshness

import com.ecoalertum.android.core.data.TestData.MINUTE
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.model.SourceStatus
import com.ecoalertum.android.core.data.repository.DataState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HazardStateTest {

    private val river = HazardReading(
        id = "river",
        type = ReadingType.FLOOD_LEVEL,
        source = Source.INFOBANJIR,
        fetchedAt = NOW - 120 * MINUTE,
    )

    private fun state(status: SourceStatus?, error: String? = null) =
        HazardState.of(HazardType.FLOOD, listOf(river), { status }, FreshnessPolicy(), NOW, error)

    @Test
    fun recentSourceSuccessKeepsAnUnchangedReadingFresh() {
        val success = state(SourceStatus(Source.INFOBANJIR, lastSuccessAt = NOW - 5 * MINUTE)) as DataState.Success

        assertFalse(success.isOutdated)
        assertEquals(NOW - 5 * MINUTE, success.lastUpdated)
    }

    @Test
    fun withoutSourceStatusFreshnessFallsBackToFetchedAt() {
        val success = state(status = null) as DataState.Success

        assertTrue(success.isOutdated)
        assertEquals(NOW - 120 * MINUTE, success.lastUpdated)
    }

    @Test
    fun readErrorKeepsTheLastReadingsAsCache() {
        val error = state(status = null, error = "PERMISSION_DENIED") as DataState.Error

        assertEquals(listOf(river), error.cachedData)
        assertEquals(NOW - 120 * MINUTE, error.lastUpdated)
    }

    @Test
    fun otherHazardsAreFilteredOut() {
        val success = HazardState.of(HazardType.HAZE, listOf(river), { null }, FreshnessPolicy(), NOW) as DataState.Success

        assertTrue(success.data.isEmpty())
        assertFalse(success.isOutdated)
    }
}
