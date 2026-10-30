package com.ecoalertum.android.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ecoalertum.android.R
import com.ecoalertum.android.core.ui.PlaceholderScreen

@Composable
fun AboutScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.menu_about), modifier = modifier)
}
