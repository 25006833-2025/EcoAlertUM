package com.ecoalertum.android.learn

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ecoalertum.android.R
import com.ecoalertum.android.core.ui.PlaceholderScreen

@Composable
fun LearnScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(title = stringResource(R.string.tab_learn), modifier = modifier)
}
