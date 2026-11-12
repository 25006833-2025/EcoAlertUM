package com.ecoalertum.android.core.data.firestore

import com.ecoalertum.android.core.data.TestData
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.FloodThresholds
import com.ecoalertum.android.core.data.model.GeoLocation
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

/** Document shapes as the backend writes them (functions/src/ingest). Dates stand in for Firestore Timestamps. */
class FirestoreMappersTest {

    private val issued = TestData.millis("2026-10-23T15:30:00+08:00")
    private val until = TestData.millis("2026-10-23T19:00:00+08:00")

    private val warningDoc = mapOf<String, Any?>(
        "type" to "weather_warning",
        "source" to "met",
        "stationId" to "20261023T1530_thunderstorms-warning",
        "stationName" to "Thunderstorms Warning",
        "location" to mapOf("lat" to 3.127447, "lng" to 101.650307),
        "stationDistanceKm" to null,
        "value" to null,
        "level" to "alert",
        "summary" to mapOf("en" to "Thunderstorms Warning for Kuala Lumpur until 19:00", "ms" to "Amaran Ribut Petir bagi Kuala Lumpur sehingga 19:00"),
        "validFrom" to Date(issued),
        "validTo" to Date(until),
        "observedAt" to Date(issued),
        "fetchedAt" to Date(issued + 60_000),
    )

    @Test
    fun mapsAWeatherWarningAsWrittenByTheBackend() {
        val reading = FirestoreMappers.toReading("met_20261023T1530_thunderstorms-warning_weather_warning", warningDoc)!!

        assertEquals(ReadingType.WEATHER_WARNING, reading.type)
        assertEquals(HazardType.RAIN, reading.type.hazardType)
        assertEquals(Source.MET, reading.source)
        assertEquals(Level.ADVISORY, reading.level)
        assertEquals(GeoLocation(3.127447, 101.650307), reading.location)
        assertNull(reading.value)
        assertEquals(until, reading.validTo)
        assertEquals(issued + 60_000, reading.fetchedAt)
        assertEquals("Amaran Ribut Petir bagi Kuala Lumpur sehingga 19:00", reading.summary.ms)
    }

    @Test
    fun mapsFloodThresholdsAndNumbersOfAnyType() {
        val reading = FirestoreMappers.toReading(
            "infobanjir_0232121WL_flood_level",
            mapOf(
                "type" to "flood_level",
                "source" to "infobanjir",
                "value" to 25.12,
                "stationDistanceKm" to 3,
                "thresholds" to mapOf("alert" to 24.7, "warning" to 26.2, "danger" to 26.7),
                "level" to "alert",
                "fetchedAt" to 1_000L,
            ),
        )!!

        assertEquals(FloodThresholds(24.7, 26.2, 26.7), reading.thresholds)
        assertEquals(3.0, reading.stationDistanceKm!!, 0.0)
        assertEquals(1_000L, reading.fetchedAt)
    }

    @Test
    fun skipsUnknownTypesAndSourcesAndDefaultsUnknownLevel() {
        assertNull(FirestoreMappers.toReading("x", warningDoc + ("type" to "earthquake")))
        assertNull(FirestoreMappers.toReading("x", warningDoc + ("source" to "nasa")))
        assertEquals(Level.NORMAL, FirestoreMappers.toReading("x", warningDoc + ("level" to "extreme"))!!.level)
    }

    @Test
    fun mapsAnAlert() {
        val alert = FirestoreMappers.toAlert(
            "Hn4W",
            mapOf(
                "hazardType" to "flood_level",
                "level" to "warning",
                "title" to mapOf("en" to "River level rising", "ms" to "Paras sungai meningkat"),
                "readingIds" to listOf("infobanjir_0232121WL_flood_level", 42),
                "status" to "active",
                "issuedAt" to Date(issued),
                "expiresAt" to Date(until),
            ),
        )!!

        assertEquals(HazardType.FLOOD, alert.hazard)
        assertEquals(Level.WARNING, alert.level)
        assertEquals(listOf("infobanjir_0232121WL_flood_level"), alert.readingIds)
        assertEquals(AlertStatus.ACTIVE, alert.status)
        assertEquals("whole_campus", alert.area)
        assertEquals(until, alert.expiresAt)
    }

    @Test
    fun mapsSourceStatusFromTheDocumentId() {
        val status = FirestoreMappers.toSourceStatus(
            "met",
            mapOf("ok" to false, "lastSuccessAt" to Date(issued), "lastAttemptAt" to Date(until), "error" to "HTTP 503"),
        )!!

        assertEquals(Source.MET, status.source)
        assertFalse(status.ok)
        assertEquals(issued, status.lastSuccessAt)
        assertEquals("HTTP 503", status.error)
        assertNull(FirestoreMappers.toSourceStatus("weather_bureau", emptyMap()))
    }

    @Test
    fun millisAcceptsDatesAndNumbersOnly() {
        assertEquals(5L, FirestoreMappers.millis(5))
        assertEquals(issued, FirestoreMappers.millis(Date(issued)))
        assertNull(FirestoreMappers.millis("2026-10-23"))
        assertNull(FirestoreMappers.millis(null))
    }
}
