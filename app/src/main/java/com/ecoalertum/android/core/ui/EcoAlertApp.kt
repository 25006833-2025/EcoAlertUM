package com.ecoalertum.android.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecoalertum.android.R
import com.ecoalertum.android.core.navigation.EcoAlertNavHost
import com.ecoalertum.android.core.navigation.Routes
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import kotlinx.coroutines.launch

/** Root composable: side menu, top bar, bottom bar and the navigation graph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoAlertApp() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Top bar, side menu and bottom bar only appear on the main screens (not on login, later).
    val inMainGraph = currentDestination?.hierarchy?.any { it.route == Routes.MAIN_GRAPH } == true

    // TODO: read the role from users/{uid} once login exists. Hides the moderator queue until then.
    val isModerator = false

    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val closeDrawer: () -> Unit = { scope.launch { drawerState.close() } }

    BackHandler(enabled = drawerState.isOpen, onBack = closeDrawer)

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = inMainGraph,
        drawerContent = {
            EcoAlertDrawer(
                currentRoute = currentDestination?.route,
                isModerator = isModerator,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) { launchSingleTop = true }
                    closeDrawer()
                },
                onClose = closeDrawer,
            )
        },
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                if (inMainGraph) {
                    TopAppBar(
                        title = { Text(stringResource(R.string.app_name)) },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_open))
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (inMainGraph) {
                    EcoAlertBottomBar(
                        currentDestination = currentDestination,
                        onTabSelected = { tab ->
                            navController.navigate(tab.route) {
                                // Standard tab behaviour: one copy of each tab, state kept when switching.
                                // Anchored to Home (not the graph start) so it still works once login is in front.
                                popUpTo(Routes.HOME) { saveState = true }
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
}

@Preview(showBackground = true)
@Composable
fun EcoAlertAppPreview() {
    EcoAlertUMTheme {
        EcoAlertApp()
    }
}
