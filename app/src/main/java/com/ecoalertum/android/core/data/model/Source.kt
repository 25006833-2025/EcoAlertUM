package com.ecoalertum.android.core.data.model

/** Official data source. [value] is the stored string and the `sourceStatus` document ID. */
enum class Source(val value: String) {
    MET("met"),
    DOE("doe"),
    INFOBANJIR("infobanjir");

    companion object {
        fun fromValue(value: String?): Source? = entries.firstOrNull { it.value == value }
    }
}
