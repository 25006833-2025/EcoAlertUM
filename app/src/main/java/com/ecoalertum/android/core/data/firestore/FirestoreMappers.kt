package com.ecoalertum.android.core.data.firestore

import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.FloodThresholds
import com.ecoalertum.android.core.data.model.GeoLocation
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.LocalizedText
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import com.ecoalertum.android.core.data.model.SourceStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.util.Date

/**
 * Firestore document data -> models. Explicit mapping instead of `toObject()`, so unknown values fall
 * back safely (docs/data-models.md) and stored strings like "alert" map to [Level.ADVISORY].
 * Takes plain maps (`snapshot.data`) so it is unit tested without Firestore.
 */
object FirestoreMappers {

    /** Null for unknown type or source, so the app skips data it does not understand. */
    fun toReading(id: String, data: Map<String, Any?>): HazardReading? {
        val type = ReadingType.fromValue(data["type"] as? String) ?: return null
        val source = Source.fromValue(data["source"] as? String) ?: return null
        return HazardReading(
            id = id,
            type = type,
            source = source,
            stationId = data["stationId"] as? String ?: "",
            stationName = data["stationName"] as? String ?: "",
            location = location(data["location"]),
            stationDistanceKm = double(data["stationDistanceKm"]),
            value = double(data["value"]),
            unit = data["unit"] as? String,
            previousValue = double(data["previousValue"]),
            thresholds = (data["thresholds"] as? Map<*, *>)?.let {
                FloodThresholds(double(it["alert"]) ?: 0.0, double(it["warning"]) ?: 0.0, double(it["danger"]) ?: 0.0)
            },
            level = Level.fromValue(data["level"] as? String),
            summary = localized(data["summary"]),
            validFrom = millis(data["validFrom"]),
            validTo = millis(data["validTo"]),
            observedAt = millis(data["observedAt"]) ?: 0L,
            fetchedAt = millis(data["fetchedAt"]) ?: 0L,
        )
    }

    fun toAlert(id: String, data: Map<String, Any?>): Alert? {
        val type = ReadingType.fromValue(data["hazardType"] as? String) ?: return null
        return Alert(
            id = id,
            hazardType = type,
            level = Level.fromValue(data["level"] as? String),
            title = localized(data["title"]),
            message = localized(data["message"]),
            area = data["area"] as? String ?: "whole_campus",
            ruleId = data["ruleId"] as? String ?: "",
            readingIds = (data["readingIds"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
            status = AlertStatus.fromValue(data["status"] as? String),
            issuedAt = millis(data["issuedAt"]) ?: 0L,
            expiresAt = millis(data["expiresAt"]) ?: 0L,
        )
    }

    /** [id] is the document ID, which is the source name (`sourceStatus/met`). */
    fun toSourceStatus(id: String, data: Map<String, Any?>): SourceStatus? {
        val source = Source.fromValue(id) ?: return null
        return SourceStatus(
            source = source,
            ok = data["ok"] as? Boolean ?: false,
            lastSuccessAt = millis(data["lastSuccessAt"]),
            lastAttemptAt = millis(data["lastAttemptAt"]),
            error = data["error"] as? String,
            stationsMissing = (data["stationsMissing"] as? List<*>)?.filterIsInstance<String>().orEmpty(),
        )
    }

    /** Firestore Timestamp, Date or epoch millis -> epoch millis. */
    fun millis(value: Any?): Long? = when (value) {
        is Timestamp -> value.toDate().time
        is Date -> value.time
        is Number -> value.toLong()
        else -> null
    }

    private fun location(value: Any?): GeoLocation = when (value) {
        is GeoPoint -> GeoLocation(value.latitude, value.longitude)
        is Map<*, *> -> GeoLocation(double(value["lat"]) ?: 0.0, double(value["lng"]) ?: 0.0)
        else -> GeoLocation()
    }

    private fun double(value: Any?): Double? = (value as? Number)?.toDouble()

    private fun localized(value: Any?): LocalizedText {
        val map = value as? Map<*, *> ?: return LocalizedText()
        return LocalizedText(map["en"] as? String ?: "", map["ms"] as? String ?: "")
    }
}
