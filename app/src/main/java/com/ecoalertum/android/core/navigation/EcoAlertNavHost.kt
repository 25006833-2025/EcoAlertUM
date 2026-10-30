package com.ecoalertum.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ecoalertum.android.alerts.AlertHistoryScreen
import com.ecoalertum.android.alerts.AlertPreferencesScreen
import com.ecoalertum.android.alerts.AlertsScreen
import com.ecoalertum.android.assistant.AssistantScreen
import com.ecoalertum.android.home.HomeScreen
import com.ecoalertum.android.learn.LearnScreen
import com.ecoalertum.android.learn.ProgressScreen
import com.ecoalertum.android.map.MapScreen
import com.ecoalertum.android.map.ModeratorQueueScreen
import com.ecoalertum.android.map.MyReportsScreen
import com.ecoalertum.android.settings.AboutScreen
import com.ecoalertum.android.settings.EmergencyContactsScreen

/**
 * The app's navigation graph.
 *
 * Login goes in front of the main screens later:
 * 1. add `composable(Routes.LOGIN) { LoginScreen(...) }` here,
 * 2. set `startDestination = Routes.LOGIN`,
 * 3. on success call `navController.navigate(Routes.MAIN_GRAPH) { popUpTo(Routes.LOGIN) { inclusive = true } }`.
 * The top bar, side menu and bottom bar only show inside [Routes.MAIN_GRAPH], so login gets a full screen.
 */
@Composable
fun EcoAlertNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.MAIN_GRAPH,
        modifier = modifier,
    ) {
        mainGraph()
    }
}

/** Everything behind login: the bottom-bar tabs and the side-menu screens. */
private fun NavGraphBuilder.mainGraph() {
    navigation(startDestination = Routes.HOME, route = Routes.MAIN_GRAPH) {
        // Bottom-bar tabs
        composable(Routes.HOME) { HomeScreen() }
        composable(Routes.ALERTS) { AlertsScreen() }
        composable(Routes.MAP) { MapScreen() }
        composable(Routes.ASSISTANT) { AssistantScreen() }
        composable(Routes.LEARN) { LearnScreen() }

        // Side-menu screens
        composable(Routes.PROGRESS) { ProgressScreen() }
        composable(Routes.MY_REPORTS) { MyReportsScreen() }
        composable(Routes.ALERT_HISTORY) { AlertHistoryScreen() }
        composable(Routes.ALERT_PREFERENCES) { AlertPreferencesScreen() }
        composable(Routes.EMERGENCY_CONTACTS) { EmergencyContactsScreen() }
        composable(Routes.ABOUT) { AboutScreen() }
        composable(Routes.MODERATOR_QUEUE) { ModeratorQueueScreen() }
    }
}
