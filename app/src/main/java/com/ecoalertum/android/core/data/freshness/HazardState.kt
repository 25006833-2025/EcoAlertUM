package com.ecoalertum.android.core.data.freshness

import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.model.SourceStatus
import com.ecoalertum.android.core.data.repository.DataState

/** Builds the [DataState] for one hazard. Shared by the fake and Firestore repositories so both follow one rule. */
object HazardState {

    /**
     * [errorMessage] non-null means the readings could not be read; [readings] is then the last saved copy.
     * `isOutdated` is true if any reading is outdated; `lastUpdated` is the oldest one.
     */
    fun of(
        type: HazardType,
        readings: List<HazardReading>,
        statusOf: (Source) -> SourceStatus?,
        policy: FreshnessPolicy,
        nowMillis: Long,
        errorMessage: String? = null,
    ): DataState<List<HazardReading>> {
        val matching = readings.filter { it.type.hazardType == type }
        val lastUpdatedTimes = matching.map { Freshness.lastUpdatedOf(it, statusOf(it.source)) }
        val lastUpdated = lastUpdatedTimes.filterNotNull().minOrNull()

        errorMessage?.let { return DataState.Error(it, matching.ifEmpty { null }, lastUpdated) }

        val isOutdated = matching.indices.any { i -> policy.isOutdated(matching[i].source, lastUpdatedTimes[i], nowMillis) }
        return DataState.Success(matching, isOutdated, lastUpdated)
    }
}
