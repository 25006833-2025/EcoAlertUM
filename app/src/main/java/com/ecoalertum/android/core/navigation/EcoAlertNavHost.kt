package com.ecoalertum.android.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.ecoalertum.android.alerts.AlertsScreen
import com.ecoalertum.android.assistant.AssistantScreen
import com.ecoalertum.android.dashboard.DashboardScreen
import com.ecoalertum.android.learn.LearnScreen
import com.ecoalertum.android.reports.ReportsScreen

/**
 * The app's navigation graph.
 *
 * Login goes in front of the tabs later:
 * 1. add `composable(Routes.LOGIN) { LoginScreen(...) }` here,
 * 2. set `startDestination = Routes.LOGIN`,
 * 3. on success call `navController.navigate(Routes.MAIN_GRAPH) { popUpTo(Routes.LOGIN) { inclusive = true } }`.
 * The bottom bar only shows inside [Routes.MAIN_GRAPH], so login gets a full screen.
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

/** The five bottom-bar tabs, grouped so the whole set can sit behind login. */
private fun NavGraphBuilder.mainGraph() {
    navigation(startDestination = Routes.DASHBOARD, route = Routes.MAIN_GRAPH) {
        composable(Routes.DASHBOARD) { DashboardScreen() }
        composable(Routes.ALERTS) { AlertsScreen() }
        composable(Routes.REPORTS) { ReportsScreen() }
        composable(Routes.ASSISTANT) { AssistantScreen() }
        composable(Routes.LEARN) { LearnScreen() }
    }
}
