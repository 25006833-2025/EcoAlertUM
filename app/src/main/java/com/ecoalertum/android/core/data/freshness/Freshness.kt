package com.ecoalertum.android.core.data.freshness

import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.SourceStatus

/** Pure "is this data too old?" rules. All times are epoch milliseconds. */
object Freshness {

    /** True when [lastUpdatedMillis] is missing or more than [maxAgeMillis] before [nowMillis]. */
    fun isOutdated(lastUpdatedMillis: Long?, maxAgeMillis: Long, nowMillis: Long): Boolean =
        lastUpdatedMillis == null || nowMillis - lastUpdatedMillis > maxAgeMillis

    /**
     * When the app last knew [reading] was current: the later of the reading's `fetchedAt` and its
     * source's `lastSuccessAt`. The backend only rewrites a reading when its value changes, so an
     * unchanged river level keeps an old `fetchedAt` even though the source was checked just now.
     * Returns null when neither time is known.
     */
    fun lastUpdatedOf(reading: HazardReading, status: SourceStatus?): Long? =
        listOfNotNull(reading.fetchedAt.takeIf { it > 0 }, status?.lastSuccessAt).maxOrNull()
}
