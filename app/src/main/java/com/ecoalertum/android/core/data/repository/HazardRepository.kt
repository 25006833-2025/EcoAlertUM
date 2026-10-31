package com.ecoalertum.android.core.data.repository

import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.HazardType
import kotlinx.coroutines.flow.Flow

/** Current official readings. Only the backend writes them; the app only reads. */
interface HazardRepository {

    /**
     * Latest readings for [type], e.g. rainfall and MET warnings for [HazardType.RAIN].
     *
     * Emits [DataState.Loading] first, then [DataState.Success] as soon as any saved data exists,
     * even when a source is down (then `isOutdated = true`), and again whenever the data changes.
     * `isOutdated` is true if any reading in the list is outdated; `lastUpdated` is the oldest one.
     * [DataState.Error] means the readings themselves could not be read.
     */
    fun observeReadings(type: HazardType): Flow<DataState<List<HazardReading>>>
}
