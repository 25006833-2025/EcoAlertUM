package com.ecoalertum.android.core.navigation

/**
 * Every route name in the app, in one place.
 * Screens never hard-code route strings; they use these constants.
 */
object Routes {
    /** Nested graph that holds the bottom-bar tabs and the side-menu screens. */
    const val MAIN_GRAPH = "main"

    // Bottom-bar tabs
    const val HOME = "home"
    const val ALERTS = "alerts"
    const val MAP = "map"
    const val ASSISTANT = "assistant"
    const val LEARN = "learn"

    // Side-menu screens
    const val PROGRESS = "progress"
    const val MY_REPORTS = "my_reports"
    const val ALERT_HISTORY = "alert_history"
    const val ALERT_PREFERENCES = "alert_preferences"
    const val EMERGENCY_CONTACTS = "emergency_contacts"
    const val ABOUT = "about"
    const val MODERATOR_QUEUE = "moderator_queue"

    // Add LOGIN = "login" here when the login screen is built.
}
