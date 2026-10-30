package com.ecoalertum.android.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.ecoalertum.android.R

/** Side-menu items that open a screen. Section order and layout live in EcoAlertDrawer. */
enum class DrawerDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    PROGRESS(Routes.PROGRESS, R.string.menu_progress, Icons.Filled.Star),
    MY_REPORTS(Routes.MY_REPORTS, R.string.menu_my_reports, Icons.Filled.LocationOn),
    ALERT_HISTORY(Routes.ALERT_HISTORY, R.string.menu_alert_history, Icons.AutoMirrored.Filled.List),
    ALERT_PREFERENCES(Routes.ALERT_PREFERENCES, R.string.menu_alert_preferences, Icons.Filled.Notifications),
    EMERGENCY_CONTACTS(Routes.EMERGENCY_CONTACTS, R.string.menu_emergency_contacts, Icons.Filled.Phone),
    ABOUT(Routes.ABOUT, R.string.menu_about, Icons.Filled.Info),
    MODERATOR_QUEUE(Routes.MODERATOR_QUEUE, R.string.menu_moderator_queue, Icons.Filled.CheckCircle),
}
