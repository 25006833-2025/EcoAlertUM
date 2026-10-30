package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.TestData
import com.ecoalertum.android.core.data.TestData.MINUTE
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.repository.DataState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class) // UnconfinedTestDispatcher, currentTime
class FakeHazardRepositoryTest {

    private val repository = FakeHazardRepository(TestData.readings(), nowMillis = { NOW })

    /** The first state after Loading. */
    private suspend fun currentState(type: HazardType): DataState<List<HazardReading>> =
        repository.observeReadings(type).take(2).toList().last()

    @Test
    fun emitsLoadingThenFreshSuccess() = runTest {
        val states = repository.observeReadings(HazardType.RAIN).take(2).toList()

        assertEquals(DataState.Loading, states[0])
        val success = states[1] as DataState.Success
        assertEquals(setOf(ReadingType.RAINFALL, ReadingType.WEATHER_WARNING), success.data.map { it.type }.toSet())
        assertFalse(success.isOutdated)
        assertEquals(NOW, success.lastUpdated)
    }

    @Test
    fun filtersReadingsByHazardType() = runTest {
        assertEquals(2, (currentState(HazardType.FLOOD) as DataState.Success).data.size)
        assertEquals(listOf("doe_CA19B_air_quality"), (currentState(HazardType.HAZE) as DataState.Success).data.map { it.id })
    }

    @Test
    fun sourceDownStillShowsSavedDataMarkedOutdated() = runTest {
        repository.simulateSourceDown(Source.INFOBANJIR, lastSuccessAt = NOW - 50 * MINUTE)

        val state = currentState(HazardType.FLOOD)

        // NFR1: a down source gives saved data with the OUTDATED flag, not an error.
        val success = state as DataState.Success
        assertEquals(2, success.data.size)
        assertTrue(success.isOutdated)
        assertEquals(NOW - 50 * MINUTE, success.lastUpdated)
    }

    @Test
    fun recentlyDownSourceIsNotOutdatedYet() = runTest {
        repository.simulateSourceDown(Source.INFOBANJIR, lastSuccessAt = NOW - 30 * MINUTE)

        assertFalse((currentState(HazardType.FLOOD) as DataState.Success).isOutdated)
    }

    @Test
    fun defaultOutageIsLongEnoughToBeOutdated() = runTest {
        repository.simulateSourceDown(Source.DOE)

        assertTrue((currentState(HazardType.HAZE) as DataState.Success).isOutdated)
    }

    @Test
    fun sourceDownOnlyAffectsItsOwnHazards() = runTest {
        repository.simulateSourceDown(Source.DOE)

        assertFalse((currentState(HazardType.FLOOD) as DataState.Success).isOutdated)
    }

    @Test
    fun restoringSourceMakesDataFreshAgain() = runTest {
        repository.simulateSourceDown(Source.INFOBANJIR)
        repository.restoreSource(Source.INFOBANJIR)

        assertFalse((currentState(HazardType.FLOOD) as DataState.Success).isOutdated)
    }

    @Test
    fun runningFlowEmitsOutdatedWhenSourceGoesDown() = runTest {
        val states = mutableListOf<DataState<List<HazardReading>>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.observeReadings(HazardType.FLOOD).toList(states)
        }

        repository.simulateSourceDown(Source.INFOBANJIR)

        assertFalse((states[1] as DataState.Success).isOutdated)
        assertTrue((states.last() as DataState.Success).isOutdated)
    }

    @Test
    fun errorKeepsCachedReadings() = runTest {
        repository.simulateError("Permission denied")

        val error = currentState(HazardType.FLOOD) as DataState.Error
        assertEquals("Permission denied", error.message)
        assertEquals(2, error.cachedData!!.size)
    }

    @Test
    fun noLoadDelayMeansDataIsAvailableImmediately() = runTest {
        val first = repository.observeReadings(HazardType.RAIN).first { it !is DataState.Loading }

        assertTrue(first is DataState.Success)
        assertEquals(0L, testScheduler.currentTime)
    }
}
