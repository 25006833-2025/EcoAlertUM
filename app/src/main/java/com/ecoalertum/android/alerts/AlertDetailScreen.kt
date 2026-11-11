package com.ecoalertum.android.alerts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.Level
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.home.LevelBadge
import com.ecoalertum.android.home.labelRes
import java.text.DateFormat
import java.util.Date

/** FR2.1: one alert in full. Opened from the Alerts list or a notification. */
@Composable
fun AlertDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: AlertDetailViewModel = viewModel(factory = AlertDetailViewModel.Factory),
) {
    val state by viewModel.alert.collectAsStateWithLifecycle()
    val alert = when (val s = state) {
        is DataState.Success -> s.data
        is DataState.Error -> s.cachedData
        DataState.Loading -> null
    }

    if (alert == null) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            val text = if (state is DataState.Loading) R.string.alerts_loading else R.string.alert_detail_not_found
            Text(stringResource(text), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header: severity, title, times
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LevelBadge(alert.level)
                Text(alert.title.get("en"), style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(R.string.alert_detail_issued, formatTime(alert.issuedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (alert.status == AlertStatus.ACTIVE && alert.expiresAt > 0) {
                    Text(
                        stringResource(R.string.alert_detail_until, formatTime(alert.expiresAt)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // The 4-level scale, with this alert's level highlighted
        Text(stringResource(R.string.alert_detail_severity), style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Level.entries.forEach { level ->
                if (level == alert.level) {
                    LevelBadge(level, Modifier.weight(1f))
                } else {
                    Text(
                        stringResource(level.labelRes),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // What to do (the alert's message)
        Text(stringResource(R.string.alert_detail_what_to_do), style = MaterialTheme.typography.titleMedium)
        Text(alert.message.get("en"), style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))