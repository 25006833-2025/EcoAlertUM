package com.ecoalertum.android.alerts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ecoalertum.android.R
import com.ecoalertum.android.core.ui.PlaceholderScreen

@Composable
fun AlertPreferencesScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.menu_alert_preferences), modifier = modifier)
}
