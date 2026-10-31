package com.ecoalertum.android.core.data.model

/**
 * Latest official reading for one station and one type. Collection `hazardReadings`.
 * Field names follow docs/data-models.md. Times are epoch milliseconds.
 */
data class HazardReading(
    val id: String = "",
    val type: ReadingType = ReadingType.RAINFALL,
    val source: Source = Source.INFOBANJIR,
    val stationId: String = "",
    val stationName: String = "",
    val location: GeoLocation = GeoLocation(),
    val stationDistanceKm: Double? = null,
    val value: Double? = null,
    val unit: String? = null,
    val previousValue: Double? = null,
    val thresholds: FloodThresholds? = null,
    val level: Level = Level.NORMAL,
    val summary: LocalizedText = LocalizedText(),
    val validFrom: Long? = null,
    val validTo: Long? = null,
    val observedAt: Long = 0L,
    val fetchedAt: Long = 0L,
)
