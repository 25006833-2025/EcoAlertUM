package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.TestData
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.DataState
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FakeAlertRepositoryTest {

    private val repository = FakeAlertRepository(TestData.alerts(), nowMillis = { NOW })

    private suspend fun currentState(type: HazardType?): DataState<List<Alert>> =
        repository.observeAlerts(type).take(2).toList().last()

    @Test
    fun emitsLoadingThenAlertsNewestFirst() = runTest {
        val states = repository.observeAlerts().take(2).toList()

        assertEquals(DataState.Loading, states[0])
        val success = states[1] as DataState.Success
        assertEquals(
            listOf("Hn4Wc8Ue2JdK6sQ0rT5v", "Xk29fQa7LmP3vR8tY1zB", "Bq7Zp1Mx9GcV3nE5hL0a"),
            success.data.map { it.id },
        )
        assertFalse(success.isOutdated)
    }

    @Test
    fun filtersByHazardType() = runTest {
        assertEquals(listOf("Xk29fQa7LmP3vR8tY1zB"), (currentState(HazardType.RAIN) as DataState.Success).data.map { it.id })
        assertEquals(listOf("Hn4Wc8Ue2JdK6sQ0rT5v"), (currentState(HazardType.FLOOD) as DataState.Success).data.map { it.id })

        val haze = (currentState(HazardType.HAZE) as DataState.Success).data
        assertEquals(listOf(AlertStatus.EXPIRED), haze.map { it.status })
    }

    @Test
    fun errorKeepsCachedAlerts() = runTest {
        repository.simulateError("Offline")

        val error = currentState(null) as DataState.Error
        assertEquals("Offline", error.message)
        assertEquals(3, error.cachedData!!.size)
    }
}
