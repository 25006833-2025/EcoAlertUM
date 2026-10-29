package com.ecoalertum.android.core.navigation

/**
 * Every route name in the app, in one place.
 * Screens never hard-code route strings; they use these constants.
 */
object Routes {
    /** Nested graph that holds the five bottom-bar tabs. */
    const val MAIN_GRAPH = "main"

    const val DASHBOARD = "dashboard"
    const val ALERTS = "alerts"
    const val REPORTS = "reports"
    const val ASSISTANT = "assistant"
    const val LEARN = "learn"

    // Add LOGIN = "login" here when the login screen is built.
}
