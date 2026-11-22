package com.ecoalertum.android.core.data.repository

import com.ecoalertum.android.core.data.fake.MockJsonParser
import com.ecoalertum.android.core.data.model.EmergencyContact

/** Where the emergency contact list comes from (FR6.2). */
interface EmergencyContactRepository {
    /** All contacts, in display order. */
    fun getContacts(): List<EmergencyContact>
}

/**
 * Reads the contacts bundled in the app (assets/mock/emergencyContacts.json).
 * Bundled on purpose: emergency numbers must open with no signal and without logging in.
 * It can move to the `emergencyContacts` Firestore collection later without changing the screen.
 */
class BundledEmergencyContactRepository(private val readJson: () -> String) : EmergencyContactRepository {
    private val loaded: List<EmergencyContact> by lazy {
        MockJsonParser.parseEmergencyContacts(readJson()).sortedBy { it.sortOrder }
    }

    override fun getContacts(): List<EmergencyContact> = loaded
}
