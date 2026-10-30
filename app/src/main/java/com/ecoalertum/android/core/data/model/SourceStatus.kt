package com.ecoalertum.android.core.data.model

/** Health of one official source. Document `sourceStatus/{source}`. Times are epoch milliseconds. */
data class SourceStatus(
    val source: Source = Source.MET,
    val ok: Boolean = true,
    val lastSuccessAt: Long? = null,
    val lastAttemptAt: Long? = null,
    val error: String? = null,
    val stationsMissing: List<String> = emptyList(),
)
