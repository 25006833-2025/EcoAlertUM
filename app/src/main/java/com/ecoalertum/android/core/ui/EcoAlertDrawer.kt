package com.ecoalertum.android.core.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R
import com.ecoalertum.android.core.navigation.DrawerDestination

/** Side menu content, laid out as in the wireframe. */
@Composable
fun EcoAlertDrawer(
    currentRoute: String?,
    isModerator: Boolean,
    onDestinationSelected: (DrawerDestination) -> Unit,
    onClose: () -> Unit,
) {
    ModalDrawerSheet {
        DrawerHeader(onClose = onClose)
        HorizontalDivider()

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionTitle(R.string.menu_section_my_stuff)
            DrawerItem(DrawerDestination.PROGRESS, currentRoute, onDestinationSelected)
            DrawerItem(DrawerDestination.MY_REPORTS, currentRoute, onDestinationSelected)
            DrawerItem(DrawerDestination.ALERT_HISTORY, currentRoute, onDestinationSelected)

            SectionTitle(R.string.menu_section_settings)
            DrawerItem(DrawerDestination.ALERT_PREFERENCES, currentRoute, onDestinationSelected)
            NavigationDrawerItem(
                label = { Text(stringResource(R.string.menu_language)) },
                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                badge = { Text(stringResource(R.string.menu_language_options)) },
                selected = false,
                onClick = { /* TODO: switch app language (EN/BM). Not a screen. */ },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )

            SectionTitle(R.string.menu_section_help)
            DrawerItem(DrawerDestination.EMERGENCY_CONTACTS, currentRoute, onDestinationSelected)
            DrawerItem(DrawerDestination.ABOUT, currentRoute, onDestinationSelected)
            if (isModerator) {
                DrawerItem(DrawerDestination.MODERATOR_QUEUE, currentRoute, onDestinationSelected)
            }
        }

        HorizontalDivider()
        NavigationDrawerItem(
            label = { Text(stringResource(R.string.menu_log_out)) },
            icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
            selected = false,
            onClick = { /* TODO (Module 6): sign out and return to login. */ },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
        )
    }
}

@Composable
private fun DrawerHeader(onClose: () -> Unit) {
    Row(
        modifier = Modifier.padding(start = 28.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Placeholder until login provides the signed-in student's details.
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.menu_placeholder_name),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.menu_placeholder_email),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.menu_close))
        }
    }
}

@Composable
private fun SectionTitle(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 16.dp, bottom = 8.dp),
    )
}

@Composable
private fun DrawerItem(
    destination: DrawerDestination,
    currentRoute: String?,
    onDestinationSelected: (DrawerDestination) -> Unit,
) {
    NavigationDrawerItem(
        label = { Text(stringResource(destination.labelRes)) },
        icon = { Icon(destination.icon, contentDescription = null) },
        selected = currentRoute == destination.route,
        onClick = { onDestinationSelected(destination) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
    )
}
