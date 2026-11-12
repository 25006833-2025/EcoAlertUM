package com.ecoalertum.android.alerts

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.R
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.home.LevelBadge
import java.text.DateFormat
import java.util.Date

/** FR2.3: every alert, newest first, filterable by hazard. */
@Composable
fun AlertsScreen(
    modifier: Modifier = Modifier,
    onAlertClick: (String) -> Unit = {},
    viewModel: AlertsViewModel = viewModel(factory = AlertsViewModel.Factory),
) {
    val state by viewModel.alerts.collectAsStateWithLifecycle()
    val selected by viewModel.selectedType.collectAsStateWithLifecycle()

    Column(modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        FilterRow(selected = selected, onSelect = viewModel::selectFilter)
        when (val s = state) {
            DataState.Loading -> CenteredText(stringResource(R.string.alerts_loading))
            // NFR1: if loading fails but we have saved alerts, still show them.
            is DataState.Error -> s.cachedData?.let { AlertList(it, onAlertClick) }
                ?: CenteredText(stringResource(R.string.alerts_error))
            is DataState.Success -> AlertList(s.data, onAlertClick)
        }
    }
}

@Composable
private fun FilterRow(selected: HazardType?, onSelect: (HazardType?) -> Unit) {
    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onSelect(null) },
            label = { Text(stringResource(R.string.alerts_filter_all)) },
        )
        HazardType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelect(type) },
                label = { Text(stringResource(type.filterLabel())) },
            )
        }
    }
}

private fun HazardType.filterLabel(): Int = when (this) {
    HazardType.RAIN -> R.string.alerts_filter_rain
    HazardType.FLOOD -> R.string.alerts_filter_flood
    HazardType.HAZE -> R.string.alerts_filter_haze
}

@Composable
private fun AlertList(alerts: List<Alert>, onAlertClick: (String) -> Unit) {
    if (alerts.isEmpty()) {
        CenteredText(stringResource(R.string.alerts_empty))
        return
    }
    // Active alerts go under "Right now", the rest under "Past alerts".
    val (active, past) = alerts.partition { it.status == AlertStatus.ACTIVE }
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 16.dp),
    ) {
        if (active.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.alerts_section_now)) }
            items(active, key = { it.id }) { AlertRow(it, onAlertClick) }
        }
        if (past.isNotEmpty()) {
            item { SectionTitle(stringResource(R.string.alerts_section_past)) }
            items(past, key = { it.id }) { AlertRow(it, onAlertClick) }
        }
    }
}

@Composable
private fun AlertRow(alert: Alert, onAlertClick: (String) -> Unit) {
    Card(onClick = { onAlertClick(alert.id) }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            LevelBadge(alert.level) // Dhanya's badge: severity word on its colour
            Text(alert.title.get("en"), style = MaterialTheme.typography.titleMedium)
            Text(
                formatTime(alert.issuedAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun CenteredText(text: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatTime(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))