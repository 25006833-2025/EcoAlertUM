package com.ecoalertum.android.core.data.freshness

import com.ecoalertum.android.core.data.model.Source

/** How old each source's data may get before the app shows it as OUTDATED. */
class FreshnessPolicy(
    private val maxAgeMillis: Map<Source, Long> = DEFAULT_MAX_AGE_MILLIS,
) {
    fun maxAgeFor(source: Source): Long = maxAgeMillis[source] ?: DEFAULT_MAX_AGE_MILLIS.getValue(source)

    fun isOutdated(source: Source, lastUpdatedMillis: Long?, nowMillis: Long): Boolean =
        Freshness.isOutdated(lastUpdatedMillis, maxAgeFor(source), nowMillis)

    companion object {
        private const val MINUTE_MILLIS = 60_000L

        /**
         * Three missed backend fetches (architecture.md): MET warnings are fetched every 5 min,
         * InfoBanjir and DOE every 15 min.
         */
        val DEFAULT_MAX_AGE_MILLIS: Map<Source, Long> = mapOf(
            Source.MET to 15 * MINUTE_MILLIS,
            Source.INFOBANJIR to 45 * MINUTE_MILLIS,
            Source.DOE to 45 * MINUTE_MILLIS,
        )
    }
}
