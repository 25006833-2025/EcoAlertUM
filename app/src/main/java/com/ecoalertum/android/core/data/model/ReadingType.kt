package com.ecoalertum.android.core.data.model

/**
 * The `type` of a reading (and the `hazardType` of an alert) as stored in Firestore.
 * More detailed than [HazardType]: rainfall and weather warnings both count as [HazardType.RAIN].
 */
enum class ReadingType(val value: String, val hazardType: HazardType) {
    RAINFALL("rainfall", HazardType.RAIN),
    WEATHER_WARNING("weather_warning", HazardType.RAIN),
    FLOOD_LEVEL("flood_level", HazardType.FLOOD),
    AIR_QUALITY("air_quality", HazardType.HAZE);

    companion object {
        /** Returns null for unknown values, so callers can skip data they don't understand. */
        fun fromValue(value: String?): ReadingType? = entries.firstOrNull { it.value == value }
    }
}
