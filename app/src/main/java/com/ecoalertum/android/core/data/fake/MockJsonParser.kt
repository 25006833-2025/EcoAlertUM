package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.FloodThresholds
import com.ecoalertum.android.core.data.model.GeoLocation
import com.ecoalertum.android.core.data.model.HazardReading
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.LocalizedText
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Turns the files in mock/ (JSON arrays, ISO 8601 times, `{lat, lng}` locations) into models.
 * Entries with an unknown type or source are skipped.
 */
object MockJsonParser {

    fun parseReadings(json: String): List<HazardReading> =
        Json.parseToJsonElement(json).jsonArray.mapNotNull { it.jsonObject.toReading() }

    fun parseAlerts(json: String): List<Alert> =
        Json.parseToJsonElement(json).jsonArray.mapNotNull { it.jsonObject.toAlert() }

    /** Parses e.g. `2026-10-23T16:05:12+08:00` to epoch millis. SimpleDateFormat works on minSdk 24. */
    fun parseTimestamp(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        return try {
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).parse(text)?.time
        } catch (e: ParseException) {
            null
        }
    }

    private fun JsonObject.toReading(): HazardReading? {
        val type = ReadingType.fromValue(string("type")) ?: return null
        val source = Source.fromValue(string("source")) ?: return null
        return HazardReading(
            id = string("id").orEmpty(),
            type = type,
            source = source,
            stationId = string("stationId").orEmpty(),
            stationName = string("stationName").orEmpty(),
            location = obj("location")?.let { GeoLocation(it.double("lat") ?: 0.0, it.double("lng") ?: 0.0) }
                ?: GeoLocation(),
            stationDistanceKm = double("stationDistanceKm"),
            value = double("value"),
            unit = string("unit"),
            previousValue = double("previousValue"),
            thresholds = obj("thresholds")?.let {
                FloodThresholds(it.double("alert") ?: 0.0, it.double("warning") ?: 0.0, it.double("danger") ?: 0.0)
            },
            level = Level.fromValue(string("level")),
            summary = localized("summary"),
            validFrom = parseTimestamp(string("validFrom")),
            validTo = parseTimestamp(string("validTo")),
            observedAt = parseTimestamp(string("observedAt")) ?: 0L,
            fetchedAt = parseTimestamp(string("fetchedAt")) ?: 0L,
        )
    }

    private fun JsonObject.toAlert(): Alert? {
        val type = ReadingType.fromValue(string("hazardType")) ?: return null
        return Alert(
            id = string("id").orEmpty(),
            hazardType = type,
            level = Level.fromValue(string("level")),
            title = localized("title"),
            message = localized("message"),
            area = string("area") ?: "whole_campus",
            ruleId = string("ruleId").orEmpty(),
            readingIds = this["readingIds"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull }.orEmpty(),
            status = AlertStatus.fromValue(string("status")),
            issuedAt = parseTimestamp(string("issuedAt")) ?: 0L,
            expiresAt = parseTimestamp(string("expiresAt")) ?: 0L,
        )
    }

    private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.double(key: String): Double? = this[key]?.jsonPrimitive?.doubleOrNull

    private fun JsonObject.localized(key: String): LocalizedText =
        obj(key)?.let { LocalizedText(it.string("en").orEmpty(), it.string("ms").orEmpty()) } ?: LocalizedText()
}
