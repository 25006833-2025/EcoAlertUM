package com.ecoalertum.android.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelTest {

    @Test
    fun levelsAreOrderedFromNormalToDanger() {
        assertEquals(listOf(Level.NORMAL, Level.ADVISORY, Level.WARNING, Level.DANGER), Level.entries.sorted())
        assertTrue(Level.NORMAL < Level.ADVISORY)
        assertTrue(Level.ADVISORY < Level.WARNING)
        assertTrue(Level.WARNING < Level.DANGER)
    }

    @Test
    fun isAtLeastComparesSeverity() {
        assertTrue(Level.DANGER.isAtLeast(Level.WARNING))
        assertTrue(Level.WARNING.isAtLeast(Level.WARNING))
        assertFalse(Level.ADVISORY.isAtLeast(Level.WARNING))
    }

    @Test
    fun highestOfPicksMostSevereOrNormalWhenEmpty() {
        assertEquals(Level.WARNING, Level.highestOf(listOf(Level.ADVISORY, Level.WARNING, Level.NORMAL)))
        assertEquals(Level.NORMAL, Level.highestOf(emptyList()))
    }

    @Test
    fun storedAlertValueMeansAdvisory() {
        assertEquals(Level.ADVISORY, Level.fromValue("alert"))
        assertEquals(Level.ADVISORY, Level.fromValue("advisory"))
        assertEquals("alert", Level.ADVISORY.value)
    }

    @Test
    fun fromValueReadsAllStoredValues() {
        assertEquals(Level.NORMAL, Level.fromValue("normal"))
        assertEquals(Level.WARNING, Level.fromValue("warning"))
        assertEquals(Level.DANGER, Level.fromValue("DANGER"))
    }

    @Test
    fun unknownOrMissingLevelIsNormal() {
        assertEquals(Level.NORMAL, Level.fromValue("extreme"))
        assertEquals(Level.NORMAL, Level.fromValue(null))
    }
}
