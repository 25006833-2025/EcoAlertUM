package com.ecoalertum.android.core.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ecoalertum.android.R
import com.ecoalertum.android.auth.AuthEvent
import com.ecoalertum.android.auth.AuthViewModel
import com.ecoalertum.android.core.navigation.EcoAlertNavHost
import com.ecoalertum.android.core.navigation.Routes
import com.ecoalertum.android.core.navigation.TopLevelDestination
import com.ecoalertum.android.home.HomeTopBarStatus
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import kotlinx.coroutines.launch

/** Root composable: side menu, top bar, bottom bar and the navigation graph. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcoAlertApp() {
    val navController = rememberNavController()

    // Module 6: one login session for the whole app. It decides where the app opens and
    // moves between the login screens and the main screens.
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.Factory)
    val startRoute = remember { authViewModel.startRoute }
    val session by authViewModel.session.collectAsStateWithLifecycle()
    LaunchedEffect(navController, authViewModel) {
        authViewModel.authEvents.collect { event ->
            val target = when (event) {
                AuthEvent.LoggedIn -> Routes.MAIN_GRAPH
                AuthEvent.NeedsVerification -> Routes.VERIFY_EMAIL
                AuthEvent.LoggedOut -> Routes.LOGIN
            }
            // Clear the back stack so Back never returns to a screen from the other side of login.
            navController.navigate(target) {
                popUpTo(navController.graph.id) { inclusive = true }
                launchSingleTop = true
            }
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // Top bar, side menu and bottom bar only appear on the main screens (not on login, later).
    val inMainGraph = currentDestination?.hierarchy?.any { it.route == Routes.MAIN_GRAPH } == true

    // Screen title in the top bar. Screens not listed here keep the app name.
    val titleRes = when (currentDestination?.route) {
        Routes.SUBMIT_REPORT -> R.string.reports_submit_title
        Routes.MAP -> R.string.reports_map_title
        Routes.MY_REPORTS -> R.string.reports_my_reports_title
        Routes.LEARN -> R.string.tab_learn
        Routes.PROGRESS -> R.string.menu_progress
        Routes.ALERT_HISTORY -> R.string.menu_alert_history
        Routes.ALERT_PREFERENCES -> R.string.menu_alert_preferences
        Routes.EMERGENCY_CONTACTS -> R.string.menu_emergency_contacts
        Routes.ABOUT -> R.string.menu_about
        Routes.MODERATOR_QUEUE -> R.string.menu_moderator_queue
        Routes.QUIZ -> R.string.quiz_title
        else -> R.string.app_name
    }

    // Bottom-bar tabs show the menu button; every other screen shows a back arrow.
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentDestination?.route }

    // The role comes from users/{uid}, read at login. Only moderators see the moderator queue.
    val isModerator = session?.isModerator == true

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
                displayName = session?.displayName,
                email = session?.email,
                onDestinationSelected = { destination ->
                    navController.navigate(destination.route) { launchSingleTop = true }
                    closeDrawer()
                },
                onLogOut = {
                    closeDrawer()
                    authViewModel.signOut()
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
                        title = { Text(stringResource(titleRes)) },
                        navigationIcon = {
                            if (isTopLevel) {
                                IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                    Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.menu_open))
                                }
                            } else {
                                IconButton(onClick = { navController.navigateUp() }) {
                                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.nav_back))
                                }
                            }
                        },
                        actions = {
                            // Home shows its "Updated" / "Offline" chip here.
                            backStackEntry?.let { entry ->
                                if (entry.destination.route == Routes.HOME) HomeTopBarStatus(entry)
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
                authViewModel = authViewModel,
                startDestination = startRoute,
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
