package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.TestData
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.FloodThresholds
import com.ecoalertum.android.core.data.model.GeoLocation
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.model.Source
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockJsonParserTest {

    @Test
    fun assetCopiesMatchMockFolder() {
        for (name in listOf("hazardReadings.json", "alerts.json")) {
            assertArrayEquals(
                "app/src/main/assets/mock/$name differs from mock/$name",
                File("../mock/$name").readBytes(),
                TestData.asset(name).readBytes(),
            )
        }
    }

    @Test
    fun parsesAllMockReadings() {
        val readings = TestData.readings()

        assertEquals(5, readings.size)
        assertEquals(
            listOf(ReadingType.WEATHER_WARNING, ReadingType.RAINFALL, ReadingType.FLOOD_LEVEL, ReadingType.FLOOD_LEVEL, ReadingType.AIR_QUALITY),
            readings.map { it.type },
        )
    }

    @Test
    fun parsesFloodReadingFields() {
        val flood = TestData.readings().single { it.id == "infobanjir_0232121WL_flood_level" }

        assertEquals(Source.INFOBANJIR, flood.source)
        assertEquals("0232121WL", flood.stationId)
        assertEquals("Sg. Klang di Bukit Putra (F2)", flood.stationName)
        assertEquals(GeoLocation(3.113333, 101.67586), flood.location)
        assertEquals(25.12, flood.value!!, 0.0)
        assertEquals(24.86, flood.previousValue!!, 0.0)
        assertEquals("m", flood.unit)
        assertEquals(FloodThresholds(alert = 24.70, warning = 26.20, danger = 26.70), flood.thresholds)
        assertEquals(Level.ADVISORY, flood.level)
        assertEquals("Paras air 25.12 m — Waspada (Amaran pada 26.20 m)", flood.summary.ms)
        assertEquals(TestData.millis("2026-10-23T16:00:00+08:00"), flood.observedAt)
        assertEquals(TestData.millis("2026-10-23T16:05:13+08:00"), flood.fetchedAt)
        assertNull(flood.validFrom)
    }

    @Test
    fun parsesWeatherWarningWithNullValues() {
        val warning = TestData.readings().single { it.type == ReadingType.WEATHER_WARNING }

        assertEquals(Source.MET, warning.source)
        assertNull(warning.value)
        assertNull(warning.unit)
        assertNull(warning.stationDistanceKm)
        assertNull(warning.thresholds)
        assertEquals(TestData.millis("2026-10-23T19:00:00+08:00"), warning.validTo)
        assertEquals(HazardType.RAIN, warning.type.hazardType)
    }

    @Test
    fun parsesAirQualityAsHazeAndNormal() {
        val air = TestData.readings().single { it.type == ReadingType.AIR_QUALITY }

        assertEquals(HazardType.HAZE, air.type.hazardType)
        assertEquals(64.0, air.value!!, 0.0)
        assertEquals(4.6, air.stationDistanceKm!!, 0.0)
        assertEquals(Level.NORMAL, air.level)
    }

    @Test
    fun parsesAllMockAlerts() {
        val alerts = TestData.alerts()

        assertEquals(3, alerts.size)
        val rain = alerts.single { it.id == "Xk29fQa7LmP3vR8tY1zB" }
        assertEquals(ReadingType.RAINFALL, rain.hazardType)
        assertEquals(HazardType.RAIN, rain.hazard)
        assertEquals(Level.ADVISORY, rain.level)
        assertEquals("Heavy rain on campus", rain.title.en)
        assertEquals("whole_campus", rain.area)
        assertEquals(2, rain.readingIds.size)
        assertEquals(AlertStatus.ACTIVE, rain.status)
        assertEquals(TestData.millis("2026-10-23T16:05:20+08:00"), rain.issuedAt)
        assertEquals(AlertStatus.EXPIRED, alerts.single { it.hazard == HazardType.HAZE }.status)
    }

    @Test
    fun skipsUnknownTypesAndDefaultsUnknownLevel() {
        val json = """
            [
              { "id": "a", "type": "earthquake", "source": "met", "level": "danger" },
              { "id": "b", "type": "rainfall", "source": "infobanjir", "level": "extreme" }
            ]
        """.trimIndent()

        val readings = MockJsonParser.parseReadings(json)

        assertEquals(listOf("b"), readings.map { it.id })
        assertEquals(Level.NORMAL, readings.single().level)
        assertEquals(0L, readings.single().fetchedAt)
    }

    @Test
    fun invalidTimestampIsNull() {
        assertNull(MockJsonParser.parseTimestamp("yesterday"))
        assertNull(MockJsonParser.parseTimestamp(null))
        assertTrue(MockJsonParser.parseTimestamp("2026-10-23T16:00:00+08:00")!! > 0)
    }
}
