package com.ecoalertum.android.map

import android.text.format.DateUtils
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R

private val StatusVerifiedColor = Color(0xFF1F7A4A)
private val StatusResolvedColor = Color(0xFF0F4C47)
private val StatusRejectedColor = Color(0xFF9F1D14)
private val StatusPendingColor = Color(0xFFB26A00)

private enum class MyReportsTab(val label: String) {
    All("All"),
    Active("Active"),
    History("History"),
}

private fun isClosedStatus(status: String): Boolean {
    val normalized = status.trim().lowercase()
    return normalized == "resolved" || normalized == "rejected"
}

private fun formatCustomCategoryLabel(rawCategory: String): String {
    val cleaned = rawCategory.trim().replace('_', ' ')
    if (cleaned.isEmpty()) return ""
    return cleaned.replaceFirstChar { it.uppercaseChar() }
}

@DrawableRes
private fun resolveCategoryIconRes(categoryKey: String): Int {
    val normalized = categoryKey.trim().lowercase()
    return when {
        normalized == ReportCategory.FALLEN_TREE.key ||
                "tree" in normalized ||
                "pokok" in normalized -> R.drawable.ic_hazard_tree

        normalized == ReportCategory.FLOODED_WALKWAY.key ||
                "flood" in normalized ||
                "water" in normalized ||
                "banjir" in normalized -> R.drawable.ic_hazard_water

        normalized == ReportCategory.BLOCKED_DRAIN.key ||
                "drain" in normalized ||
                "longkang" in normalized -> R.drawable.ic_hazard_drain

        else -> R.drawable.ic_hazard_custom
    }
}

@Composable
fun MyReportsScreen(
    modifier: Modifier = Modifier,
    onAddReport: () -> Unit = {},
) {
    val uid = remember { currentUid() }
    var reports by remember { mutableStateOf<List<HazardReport>?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var selectedTab by rememberSaveable { mutableStateOf(MyReportsTab.Active) }

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
    val filteredList = remember(list, selectedTab) {
        if (list == null) {
            emptyList()
        } else {
            when (selectedTab) {
                MyReportsTab.All -> list
                MyReportsTab.Active -> list.filterNot { isClosedStatus(it.status) }
                MyReportsTab.History -> list.filter { isClosedStatus(it.status) }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        when {
            loadFailed -> CenteredMessage(R.string.reports_load_failed)
            list == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            else -> Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MyReportsTab.entries.forEach { tab ->
                        FilterChip(
                            selected = selectedTab == tab,
                            onClick = { selectedTab = tab },
                            label = { Text(tab.label, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                            modifier = Modifier.heightIn(min = 40.dp),
                        )
                    }
                }

                if (filteredList.isEmpty()) {
                    val emptyText = when (selectedTab) {
                        MyReportsTab.All -> stringResource(R.string.reports_empty)
                        MyReportsTab.Active -> "No active reports."
                        MyReportsTab.History -> "No resolved or rejected reports in history."
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = emptyText,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            start = 16.dp,
                            top = 4.dp,
                            end = 16.dp,
                            bottom = 88.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(filteredList, key = { it.id }) { report ->
                            ReportCard(report)
                        }
                    }
                }
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
        .firstOrNull { !it.isCustom && it.key == report.category }
        ?.labelRes
        ?.let { stringResource(it) }
        ?: formatCustomCategoryLabel(report.category)

    val title = if (report.locationLabel.isBlank()) {
        categoryLabel
    } else {
        "$categoryLabel - ${report.locationLabel}"
    }

    val statusLower = report.status.trim().lowercase()
    val statusLabel = stringResource(
        when (statusLower) {
            "verified" -> R.string.reports_status_verified
            "resolved" -> R.string.reports_status_resolved
            "rejected" -> R.string.reports_status_rejected
            else -> R.string.reports_status_unverified
        }
    )

    val statusColor = when (statusLower) {
        "verified" -> StatusVerifiedColor
        "resolved" -> StatusResolvedColor
        "rejected" -> StatusRejectedColor
        else -> StatusPendingColor
    }

    val iconRes = remember(report.category) { resolveCategoryIconRes(report.category) }

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
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = statusColor,
                modifier = Modifier.size(28.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.reports_status_line, statusLabel),
                    style = MaterialTheme.typography.bodyMedium,
                    color = statusColor,
                    fontWeight = FontWeight.SemiBold,
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