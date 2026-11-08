package com.ecoalertum.android.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R

/**
 * "Updated <time>" or "Offline" chip for the top bar while Home is open.
 * [homeEntry] is Home's back stack entry, so this shares the [HomeViewModel] that [HomeScreen] uses.
 */
@Composable
fun HomeTopBarStatus(homeEntry: ViewModelStoreOwner, modifier: Modifier = Modifier) {
    val viewModel: HomeViewModel = viewModel(viewModelStoreOwner = homeEntry, factory = HomeViewModel.Factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val content = state as? HomeUiState.Content ?: return
    val formatTime = rememberTimeFormatter()

    val text = when {
        content.isOutdated -> stringResource(R.string.home_chip_offline)
        content.updatedAt != null -> stringResource(R.string.home_chip_updated, formatTime(content.updatedAt))
        else -> return
    }
    Surface(
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = Color.Transparent,
        modifier = modifier.padding(end = 12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
