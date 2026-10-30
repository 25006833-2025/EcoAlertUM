package com.ecoalertum.android.core.data.model

/**
 * The three hazards the app shows to students (Home cards, alert filters).
 * Each [ReadingType] belongs to one of them.
 */
enum class HazardType {
    /** Rain gauge readings and MET weather warnings. */
    RAIN,

    /** River water levels. */
    FLOOD,

    /** Air Pollutant Index (haze). */
    HAZE,
}
