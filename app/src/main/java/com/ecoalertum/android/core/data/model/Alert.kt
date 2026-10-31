package com.ecoalertum.android.core.data.model

/**
 * An alert raised by the backend rules. Collection `alerts`.
 * Field names follow docs/data-models.md. Times are epoch milliseconds.
 */
data class Alert(
    val id: String = "",
    /** Stored as a reading type (e.g. `rainfall`), as in the docs. See [hazard] for the app-level hazard. */
    val hazardType: ReadingType = ReadingType.RAINFALL,
    val level: Level = Level.ADVISORY,
    val title: LocalizedText = LocalizedText(),
    val message: LocalizedText = LocalizedText(),
    val area: String = "whole_campus",
    val ruleId: String = "",
    val readingIds: List<String> = emptyList(),
    val status: AlertStatus = AlertStatus.ACTIVE,
    val issuedAt: Long = 0L,
    val expiresAt: Long = 0L,
) {
    /** The app-level hazard this alert belongs to. */
    val hazard: HazardType get() = hazardType.hazardType
}
