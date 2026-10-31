package com.ecoalertum.android.core.data.model

/**
 * InfoBanjir river thresholds in metres. Field names match the stored data:
 * [alert] (Waspada) is where [Level.ADVISORY] starts.
 */
data class FloodThresholds(
    val alert: Double = 0.0,
    val warning: Double = 0.0,
    val danger: Double = 0.0,
)
