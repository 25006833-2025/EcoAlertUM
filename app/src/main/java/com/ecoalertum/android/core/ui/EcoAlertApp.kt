package com.ecoalertum.android.core.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecoalertum.android.core.navigation.EcoAlertNavHost
import com.ecoalertum.android.core.navigation.Routes
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme

/** Root composable: the scaffold, the bottom bar and the navigation graph. */
@Composable
fun EcoAlertApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = currentDestination?.hierarchy?.any { it.route == Routes.MAIN_GRAPH } == true

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                EcoAlertBottomBar(
                    currentDestination = currentDestination,
                    onTabSelected = { tab ->
                        navController.navigate(tab.route) {
                            // Standard tab behaviour: one copy of each tab, state kept when switching.
                            // Anchored to Dashboard (not the graph start) so it still works once login is in front.
                            popUpTo(Routes.DASHBOARD) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        EcoAlertNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EcoAlertAppPreview() {
    EcoAlertUMTheme {
        EcoAlertApp()
    }
}
