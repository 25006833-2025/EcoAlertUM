package com.ecoalertum.android.learn

import com.ecoalertum.android.core.data.TestData.MINUTE
import com.ecoalertum.android.core.data.TestData.NOW
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.repository.DataState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR5.2: which lesson the Learn tab puts first for the current alert. */
class LearnActiveTopicTest {

    private fun alert(
        type: ReadingType,
        level: Level = Level.WARNING,
        status: AlertStatus = AlertStatus.ACTIVE,
        expiresAt: Long = NOW + 60 * MINUTE,
    ) = Alert(id = type.value, hazardType = type, level = level, status = status, expiresAt = expiresAt)

    @Test
    fun eachAlertTypeMapsToItsLesson() {
        assertEquals("floods", LearnViewModel.topicFor(ReadingType.RAINFALL))
        assertEquals("floods", LearnViewModel.topicFor(ReadingType.FLOOD_LEVEL))
        assertEquals("haze", LearnViewModel.topicFor(ReadingType.AIR_QUALITY))
        assertEquals("storms", LearnViewModel.topicFor(ReadingType.WEATHER_WARNING))
    }

    @Test
    fun everyAlertTypeHasALearnTopic() {
        ReadingType.entries.forEach { type ->
            val topic = LearnViewModel.topicFor(type)
            assertTrue("$type -> $topic", LearnTopic.entries.any { it.id == topic })
        }
    }

    @Test
    fun picksTheMostSevereActiveAlert() {
        val alerts = listOf(
            alert(ReadingType.AIR_QUALITY, Level.ADVISORY),
            alert(ReadingType.WEATHER_WARNING, Level.DANGER),
            alert(ReadingType.RAINFALL, Level.WARNING),
        )
        assertEquals(ReadingType.WEATHER_WARNING, LearnViewModel.activeAlert(alerts, NOW)?.hazardType)
    }

    @Test
    fun ignoresExpiredCancelledAndPastExpiryAlerts() {
        val alerts = listOf(
            alert(ReadingType.RAINFALL, Level.DANGER, status = AlertStatus.EXPIRED),
            alert(ReadingType.FLOOD_LEVEL, Level.DANGER, status = AlertStatus.CANCELLED),
            alert(ReadingType.WEATHER_WARNING, Level.DANGER, expiresAt = NOW - MINUTE),
            alert(ReadingType.AIR_QUALITY, Level.ADVISORY),
        )
        assertEquals(ReadingType.AIR_QUALITY, LearnViewModel.activeAlert(alerts, NOW)?.hazardType)
    }

    @Test
    fun alertWithNoEndTimeStaysActive() {
        val alerts = listOf(alert(ReadingType.RAINFALL, expiresAt = 0L))
        assertEquals(ReadingType.RAINFALL, LearnViewModel.activeAlert(alerts, NOW)?.hazardType)
    }

    @Test
    fun noActiveAlertMeansNoHighlight() {
        assertNull(LearnViewModel.activeAlert(emptyList(), NOW))
        assertNull(LearnViewModel.activeAlert(listOf(alert(ReadingType.RAINFALL, status = AlertStatus.EXPIRED)), NOW))
    }

    @Test
    fun loadingAndErrorStatesDoNotCrash() {
        val saved = listOf(alert(ReadingType.AIR_QUALITY))
        assertTrue(LearnViewModel.alertsFrom(DataState.Loading).isEmpty())
        assertTrue(LearnViewModel.alertsFrom(DataState.Error("offline")).isEmpty())
        assertEquals(saved, LearnViewModel.alertsFrom(DataState.Error("offline", cachedData = saved)))
        assertEquals(saved, LearnViewModel.alertsFrom(DataState.Success(saved, isOutdated = false, lastUpdated = NOW)))
    }
}
