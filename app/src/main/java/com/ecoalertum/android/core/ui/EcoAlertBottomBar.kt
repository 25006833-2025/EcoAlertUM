package com.ecoalertum.android.core.ui

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hierarchy
import com.ecoalertum.android.core.navigation.TopLevelDestination

/** Material 3 bottom navigation bar built from [TopLevelDestination]. */
@Composable
fun EcoAlertBottomBar(
    currentDestination: NavDestination?,
    onTabSelected: (TopLevelDestination) -> Unit,
) {
    NavigationBar {
        TopLevelDestination.entries.forEach { tab ->
            val label = stringResource(tab.labelRes)
            NavigationBarItem(
                selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                onClick = { onTabSelected(tab) },
                icon = { Icon(tab.icon, contentDescription = null) },
                label = { Text(label) },
            )
        }
    }
}
