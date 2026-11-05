package com.ecoalertum.android.map

import android.text.format.DateUtils
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R

@Composable
fun MyReportsScreen(
    modifier: Modifier = Modifier,
    onAddReport: () -> Unit = {},
) {
    val uid = remember { currentUid() }
    var reports by remember { mutableStateOf<List<HazardReport>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }

    // Listen only while this screen is open, then stop, so no extra reads are used.
    DisposableEffect(uid) {
        val registration = HazardReportRepository().listenToMyReports(
            uid = uid,
            onSuccess = {
                reports = it
                loadFailed = false
            },
            onError = { loadFailed = true },
        )
        onDispose { registration.remove() }
    }

    val list = reports
    Box(modifier = modifier.fillMaxSize()) {
        when {
            loadFailed -> CenteredMessage(R.string.reports_load_failed)
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            list.isEmpty() -> CenteredMessage(R.string.reports_empty)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list) { report -> ReportCard(report) }
            }
        }

        FloatingActionButton(
            onClick = onAddReport,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.reports_add_report),
            )
        }
    }
}

@Composable
private fun ReportCard(report: HazardReport) {
    val categoryLabel = ReportCategory.entries
        .firstOrNull { it.key == report.category }
        ?.let { stringResource(it.labelRes) }
        ?: report.category
    val title = if (report.locationLabel.isBlank()) {
        categoryLabel
    } else {
        "$categoryLabel - ${report.locationLabel}"
    }
    val statusLabel = stringResource(
        when (report.status) {
            "verified" -> R.string.reports_status_verified
            "resolved" -> R.string.reports_status_resolved
            "rejected" -> R.string.reports_status_rejected
            else -> R.string.reports_status_unverified
        }
    )
    val timeLabel = report.createdAt?.toDate()?.time?.let { millis ->
        DateUtils.getRelativeTimeSpanString(
            millis,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
        ).toString()
    } ?: stringResource(R.string.reports_time_now)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = stringResource(R.string.reports_status_line, statusLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.reports_time_line, timeLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CenteredMessage(@StringRes textRes: Int) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = stringResource(textRes), style = MaterialTheme.typography.bodyLarge)
    }
}