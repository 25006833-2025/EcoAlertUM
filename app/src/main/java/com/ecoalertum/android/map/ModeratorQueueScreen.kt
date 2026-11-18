package com.ecoalertum.android.map

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

// Design System Tokens
private val PrimaryTeal = Color(0xFF0F4C47)
private val TealTint = Color(0xFFE3F0EC)
private val LeafGreen = Color(0xFF1F7A4A)
private val AlertAmber = Color(0xFFF2A23A)
private val DangerRed = Color(0xFF9F1D14)
private val InkDark = Color(0xFF10201E)
private val MutedText = Color(0xFF4A5B58)
private val BorderLine = Color(0xFFDDE5E2)
private val ScreenBg = Color(0xFFF4F7F5)

private enum class QueueFilter(val label: String) {
    PENDING("Pending"),
    VERIFIED("Verified"),
    ALL("All Reports")
}

@Composable
fun ModeratorQueueScreen(
    modifier: Modifier = Modifier,
    repository: HazardReportRepository = remember { HazardReportRepository() }
) {
    var reports by remember { mutableStateOf<List<HazardReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(QueueFilter.PENDING) }
    var updatingReportId by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(repository) {
        val registration = repository.getReportsStream(
            onSuccess = { fetched ->
                reports = fetched
                isLoading = false
                errorMessage = null
            },
            onError = { e ->
                errorMessage = e.localizedMessage ?: "Failed to load reports"
                isLoading = false
            }
        )
        onDispose { registration.remove() }
    }

    val filteredReports = remember(reports, selectedFilter) {
        when (selectedFilter) {
            QueueFilter.PENDING -> reports.filter {
                it.status.equals("unverified", ignoreCase = true) ||
                        it.status.equals("pending", ignoreCase = true)
            }
            QueueFilter.VERIFIED -> reports.filter {
                it.status.equals("verified", ignoreCase = true)
            }
            QueueFilter.ALL -> reports
        }
    }

    fun handleStatusChange(report: HazardReport, newStatus: String, successToast: String) {
        coroutineScope.launch {
            updatingReportId = report.id
            val result = repository.updateReportStatus(report.id, newStatus)
            updatingReportId = null
            result.fold(
                onSuccess = { snackbarHostState.showSnackbar(successToast) },
                onFailure = { err ->
                    snackbarHostState.showSnackbar(
                        "Action failed: ${err.localizedMessage ?: "Check permissions"}"
                    )
                }
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScreenBg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(QueueFilter.entries) { filter ->
                    val count = when (filter) {
                        QueueFilter.PENDING -> reports.count {
                            it.status.equals("unverified", true) || it.status.equals("pending", true)
                        }
                        QueueFilter.VERIFIED -> reports.count { it.status.equals("verified", true) }
                        QueueFilter.ALL -> reports.size
                    }
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = "${filter.label} ($count)",
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryTeal,
                            selectedLabelColor = Color.White,
                            containerColor = TealTint,
                            labelColor = PrimaryTeal
                        ),
                        modifier = Modifier.heightIn(min = 48.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryTeal)
                    }
                }
                errorMessage != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = errorMessage ?: "Error loading queue",
                            color = DangerRed,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                filteredReports.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No ${selectedFilter.label.lowercase()} reports right now.",
                            color = MutedText,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredReports, key = { it.id }) { report ->
                            ModeratorReportCard(
                                report = report,
                                isBusy = updatingReportId == report.id,
                                onVerify = {
                                    handleStatusChange(report, "verified", "Report verified & published to map")
                                },
                                onResolve = {
                                    handleStatusChange(report, "resolved", "Hazard marked as resolved")
                                },
                                onReject = {
                                    handleStatusChange(report, "rejected", "Report rejected")
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ModeratorReportCard(
    report: HazardReport,
    isBusy: Boolean,
    onVerify: () -> Unit,
    onResolve: () -> Unit,
    onReject: () -> Unit
) {
    val statusLower = report.status.lowercase()
    val (badgeBg, badgeText, statusLabel) = when (statusLower) {
        "verified" -> Triple(LeafGreen.copy(alpha = 0.14f), LeafGreen, "Verified")
        "resolved" -> Triple(PrimaryTeal.copy(alpha = 0.14f), PrimaryTeal, "Resolved")
        "rejected" -> Triple(DangerRed.copy(alpha = 0.14f), DangerRed, "Rejected")
        else -> Triple(AlertAmber.copy(alpha = 0.22f), InkDark, "Unverified")
    }

    val timeAgo = remember(report.createdAt) {
        report.createdAt?.toDate()?.time?.let { millis ->
            DateUtils.getRelativeTimeSpanString(
                millis,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS
            ).toString()
        } ?: "Just now"
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, BorderLine),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row: Category + Status Pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = report.category.ifBlank { "General Hazard" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = InkDark,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = badgeBg,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = badgeText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // AI Suggestion Pill (if Module 4 categorized it)
            if (!report.aiCategory.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                val confPercent = report.aiConfidence?.let { " (${(it * 100).toInt()}%)" }.orEmpty()
                Text(
                    text = "AI Suggested: ${report.aiCategory}$confPercent",
                    style = MaterialTheme.typography.labelMedium,
                    color = PrimaryTeal,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Location & Timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MutedText,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${report.locationLabel.ifBlank { "UM Campus" }} • $timeAgo",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedText
                )
            }

            // Description
            if (report.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkDark
                )
            }
/*
            // Evidence Photo (if available)
            if (!report.photoPath.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                AsyncImage(
                    model = report.photoPath,
                    contentDescription = "Hazard evidence photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(ScreenBg)
                )
            }
*/
            Spacer(modifier = Modifier.height(14.dp))

            // Moderator Action Buttons (48dp touch targets)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (statusLower != "verified" && statusLower != "resolved") {
                    Button(
                        onClick = onVerify,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Publish", fontWeight = FontWeight.SemiBold)
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (statusLower != "resolved") {
                        OutlinedButton(
                            onClick = onResolve,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, LeafGreen),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = LeafGreen),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resolve", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    if (statusLower != "rejected") {
                        OutlinedButton(
                            onClick = onReject,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, DangerRed),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Reject", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}