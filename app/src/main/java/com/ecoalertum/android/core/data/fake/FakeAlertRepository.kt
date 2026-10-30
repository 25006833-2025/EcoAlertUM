package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.DataState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/** In-memory [AlertRepository] backed by mock alerts. Call [simulateError] to see the error state. */
class FakeAlertRepository(
    alerts: List<Alert>,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val loadDelayMillis: Long = 0L,
) : AlertRepository {

    private data class State(val alerts: List<Alert>, val errorMessage: String? = null)

    private val state = MutableStateFlow(State(alerts))

    /** Makes every flow emit [DataState.Error] with the cached alerts. Pass null to clear. */
    fun simulateError(message: String?) {
        state.update { it.copy(errorMessage = message) }
    }

    override fun observeAlerts(type: HazardType?): Flow<DataState<List<Alert>>> = flow {
        emit(DataState.Loading)
        if (loadDelayMillis > 0) delay(loadDelayMillis)
        emitAll(state.map { it.toDataState(type) })
    }

    private fun State.toDataState(type: HazardType?): DataState<List<Alert>> {
        val matching = alerts
            .filter { type == null || it.hazard == type }
            .sortedByDescending { it.issuedAt }
        errorMessage?.let { return DataState.Error(it, matching.ifEmpty { null }) }
        // Alerts are pushed by the backend, so the fake list is never outdated.
        return DataState.Success(matching, isOutdated = false, lastUpdated = nowMillis())
    }
}
