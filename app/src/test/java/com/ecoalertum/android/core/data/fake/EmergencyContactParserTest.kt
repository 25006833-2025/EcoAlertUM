package com.ecoalertum.android.core.data.fake

import com.ecoalertum.android.core.data.TestData
import com.ecoalertum.android.core.data.model.EmergencyCategory
import com.ecoalertum.android.core.data.repository.BundledEmergencyContactRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** FR6.2: the bundled emergency contact list and how it is read. */
class EmergencyContactParserTest {

    private val bundled = TestData.asset("emergencyContacts.json").readText()

    @Test
    fun parsesAllBundledContacts() {
        val contacts = MockJsonParser.parseEmergencyContacts(bundled)
        assertEquals(6, contacts.size)
        assertEquals(
            setOf(
                EmergencyCategory.POLICE, EmergencyCategory.FIRE_RESCUE, EmergencyCategory.CIVIL_DEFENCE,
                EmergencyCategory.CAMPUS_SECURITY, EmergencyCategory.MEDICAL, EmergencyCategory.FACILITIES,
            ),
            contacts.map { it.category }.toSet(),
        )
    }

    @Test
    fun listIncludesCampusSecurityAndMedicalCentre() {
        val contacts = MockJsonParser.parseEmergencyContacts(bundled)
        assertTrue(contacts.any { it.category == EmergencyCategory.CAMPUS_SECURITY && it.phone.isNotBlank() })
        assertTrue(contacts.any { it.category == EmergencyCategory.MEDICAL && it.phone.isNotBlank() })
    }

    @Test
    fun readsNumbersHoursAndTranslations() {
        val contacts = MockJsonParser.parseEmergencyContacts(bundled).associateBy { it.id }

        val national = contacts.getValue("national_emergency")
        assertEquals("999", national.phone)
        assertEquals("112", national.altPhone)
        assertTrue(national.available24h)
        assertNull(national.hours)
        assertEquals("Kecemasan (Polis / Ambulans)", national.name.get("ms"))

        val clinic = contacts.getValue("um_health_centre")
        assertFalse(clinic.available24h)
        assertEquals("Mon\u2013Fri 08:00\u201317:00", clinic.hours)
    }

    @Test
    fun repositoryReturnsContactsInDisplayOrder() {
        val json = """
            [
              {"id":"b","name":{"en":"B"},"category":"medical","phone":"2","sortOrder":2},
              {"id":"a","name":{"en":"A"},"category":"police","phone":"1","sortOrder":1}
            ]
        """.trimIndent()
        val ids = BundledEmergencyContactRepository { json }.getContacts().map { it.id }
        assertEquals(listOf("a", "b"), ids)
    }

    @Test
    fun skipsEntriesWithUnknownCategoryOrNoPhone() {
        val json = """
            [
              {"id":"ok","name":{"en":"OK"},"category":"police","phone":"999"},
              {"id":"bad_category","name":{"en":"X"},"category":"pizza","phone":"1"},
              {"id":"no_phone","name":{"en":"Y"},"category":"medical","phone":"  "},
              {"id":"missing_phone","name":{"en":"Z"},"category":"medical"}
            ]
        """.trimIndent()
        assertEquals(listOf("ok"), MockJsonParser.parseEmergencyContacts(json).map { it.id })
    }

    @Test
    fun usesEnglishWhenThereIsNoMalayName() {
        val json = """[{"id":"a","name":{"en":"Security"},"category":"campus_security","phone":"1"}]"""
        val contact = MockJsonParser.parseEmergencyContacts(json).single()
        assertEquals("Security", contact.name.get("ms"))
    }
}
