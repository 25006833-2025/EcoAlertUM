package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.freshness.Freshness
import com.ecoalertum.android.core.data.freshness.FreshnessPolicy
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.model.SourceStatus
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.core.data.repository.HazardRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * In-memory [HazardRepository] backed by mock readings.
 *
 * Healthy sources count as checked "just now". Call [simulateSourceDown] to see the OUTDATED
 * state, and [simulateError] to see an error with cached data.
 */
class FakeHazardRepository(
    readings: List<HazardReading>,
    private val freshnessPolicy: FreshnessPolicy = FreshnessPolicy(),
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val loadDelayMillis: Long = 0L,
) : HazardRepository {

    private data class State(
        val readings: List<HazardReading>,
        /** Down sources, with their last successful fetch time. */
        val downSources: Map<Source, Long> = emptyMap(),
        val errorMessage: String? = null,
    )

    private val state = MutableStateFlow(State(readings))

    /** Pretends the backend has not reached [source] since [lastSuccessAt] (default: 2 hours ago). */
    fun simulateSourceDown(source: Source, lastSuccessAt: Long = nowMillis() - TWO_HOURS_MILLIS) {
        state.update { it.copy(downSources = it.downSources + (source to lastSuccessAt)) }
    }

    fun restoreSource(source: Source) {
        state.update { it.copy(downSources = it.downSources - source) }
    }

    /** Makes every flow emit [DataState.Error] with the cached readings. Pass null to clear. */
    fun simulateError(message: String?) {
        state.update { it.copy(errorMessage = message) }
    }

    override fun observeReadings(type: HazardType): Flow<DataState<List<HazardReading>>> = flow {
        emit(DataState.Loading)
        if (loadDelayMillis > 0) delay(loadDelayMillis)
        emitAll(state.map { it.toDataState(type) })
    }

    private fun State.toDataState(type: HazardType): DataState<List<HazardReading>> {
        val now = nowMillis()
        val matching = readings.filter { it.type.hazardType == type }
        val lastUpdatedTimes = matching.map { Freshness.lastUpdatedOf(it, statusOf(it.source, now)) }
        val lastUpdated = lastUpdatedTimes.filterNotNull().minOrNull()

        errorMessage?.let { return DataState.Error(it, matching.ifEmpty { null }, lastUpdated) }

        val isOutdated = matching.indices.any { i ->
            freshnessPolicy.isOutdated(matching[i].source, lastUpdatedTimes[i], now)
        }
        return DataState.Success(matching, isOutdated, lastUpdated)
    }

    private fun State.statusOf(source: Source, now: Long): SourceStatus {
        val downSince = downSources[source]
        return if (downSince == null) {
            SourceStatus(source, ok = true, lastSuccessAt = now, lastAttemptAt = now)
        } else {
            SourceStatus(source, ok = false, lastSuccessAt = downSince, lastAttemptAt = now, error = "Simulated outage")
        }
    }

    private companion object {
        const val TWO_HOURS_MILLIS = 2 * 60 * 60 * 1000L
    }
}
