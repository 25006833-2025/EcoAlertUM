package com.ecoalertum.android.core.data.model

/** A point on the map. Firestore stores it as a GeoPoint. */
data class GeoLocation(
    val lat: Double = 0.0,
    val lng: Double = 0.0,
)
