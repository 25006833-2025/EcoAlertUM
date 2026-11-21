package com.ecoalertum.android.core.data.model

/** The `category` of an emergency contact as stored in Firestore and mock/emergencyContacts.json. */
enum class EmergencyCategory(val value: String) {
    CAMPUS_SECURITY("campus_security"),
    MEDICAL("medical"),
    FIRE_RESCUE("fire_rescue"),
    POLICE("police"),
    CIVIL_DEFENCE("civil_defence"),
    FACILITIES("facilities");

    companion object {
        /** Returns null for unknown values, so callers can skip data they don't understand. */
        fun fromValue(value: String?): EmergencyCategory? = entries.firstOrNull { it.value == value }
    }
}

/** One official emergency contact (FR6.2). Follows docs/data-models.md section 6. */
data class EmergencyContact(
    val id: String,
    val name: LocalizedText,
    val category: EmergencyCategory,
    /** E.164 (`+603...`) or a short code (`999`). */
    val phone: String,
    val altPhone: String? = null,
    val available24h: Boolean = false,
    /** When not open 24 hours, e.g. "Mon-Fri 08:00-17:00". */
    val hours: String? = null,
    val location: String? = null,
    val sortOrder: Int = 0,
    val sourceUrl: String? = null,
    val sourceCheckedAt: Long? = null,
)
