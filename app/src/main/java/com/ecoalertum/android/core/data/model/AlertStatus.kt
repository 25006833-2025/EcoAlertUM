package com.ecoalertum.android.core.data.model

enum class AlertStatus(val value: String) {
    ACTIVE("active"),
    EXPIRED("expired"),
    CANCELLED("cancelled");

    companion object {
        /** Unknown values become [EXPIRED], so an alert is never shown as active by mistake. */
        fun fromValue(value: String?): AlertStatus = entries.firstOrNull { it.value == value } ?: EXPIRED
    }
}
