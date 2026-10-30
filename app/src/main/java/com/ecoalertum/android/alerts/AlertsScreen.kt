package com.ecoalertum.android.alerts

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ecoalertum.android.R
import com.ecoalertum.android.core.ui.PlaceholderScreen

@Composable
fun AlertsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.tab_alerts), modifier = modifier)
}
