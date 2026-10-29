package com.ecoalertum.android.core.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.ui.graphics.vector.ImageVector
import com.ecoalertum.android.R

/** The five tabs in the bottom navigation bar, in display order. */
enum class TopLevelDestination(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    DASHBOARD(Routes.DASHBOARD, R.string.tab_dashboard, Icons.Filled.Home),
    ALERTS(Routes.ALERTS, R.string.tab_alerts, Icons.Filled.Notifications),
    REPORTS(Routes.REPORTS, R.string.tab_reports, Icons.Filled.Place),
    ASSISTANT(Routes.ASSISTANT, R.string.tab_assistant, Icons.Filled.Face),
    LEARN(Routes.LEARN, R.string.tab_learn, Icons.Filled.Info),
}
