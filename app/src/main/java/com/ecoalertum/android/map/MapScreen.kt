package com.ecoalertum.android.map

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// Official Design System Tokens (from EcoAlert UM design system)
private val PrimaryTeal = Color(0xFF0F4C47)
private val TealTint = Color(0xFFE3F0EC)
private val LeafGreen = Color(0xFF1F7A4A)
private val AlertAmber = Color(0xFFF2A23A)
private val WarningOrange = Color(0xFFC2410C)
private val DangerRed = Color(0xFF9F1D14)
private val InkDark = Color(0xFF10201E)
private val MutedText = Color(0xFF4A5B58)
private val BorderLine = Color(0xFFDDE5E2)
private val ScreenBg = Color(0xFFF4F7F5)

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    repository: HazardReportRepository = remember { HazardReportRepository() },
    onNavigateToAssistant: (String) -> Unit = {}
) {
    var reports by remember { mutableStateOf<List<HazardReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf("All") }
    var selectedReport by remember { mutableStateOf<HazardReport?>(null) }

    DisposableEffect(repository) {
        val registration = repository.getReportsStream(
            onSuccess = { fetched ->
                reports = fetched
                if (selectedReport == null && reports.isNotEmpty()) {
                    selectedReport = reports.first()
                }
                isLoading = false
                errorMessage = null
            },
            onError = { e ->
                errorMessage = e.localizedMessage ?: "Failed to load map incidents"
                isLoading = false
            }
        )
        onDispose { registration.remove() }
    }

    val filterChips = listOf("All", "Weather", "Flood", "Air Quality", "My Reports", "Safe Path")

    val displayedReports = remember(reports, selectedFilter) {
        if (selectedFilter == "All") reports
        else reports.filter { it.category.contains(selectedFilter, ignoreCase = true) }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = ScreenBg
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Simulated Campus Map Canvas Background
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Top Filter Chips (Matching Wireframe 3.1)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterChips) { chip ->
                        FilterChip(
                            selected = selectedFilter == chip,
                            onClick = { selectedFilter = chip },
                            label = { Text(chip, fontWeight = FontWeight.SemiBold) },
                            shape = RoundedCornerShape(14.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryTeal,
                                selectedLabelColor = Color.White,
                                containerColor = TealTint,
                                labelColor = PrimaryTeal
                            ),
                            modifier = Modifier.heightIn(min = 40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Map Canvas Area with Hazard Pins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator(
                                color = PrimaryTeal,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        errorMessage != null -> {
                            Text(
                                text = errorMessage ?: "",
                                color = DangerRed,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }
                        displayedReports.isEmpty() -> {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Icon(Icons.Filled.Info, contentDescription = null, tint = MutedText, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No hazards found on campus map.", color = MutedText, fontWeight = FontWeight.Medium)
                            }
                        }
                        else -> {
                            Box(modifier = Modifier.fillMaxSize()) {
                                displayedReports.forEachIndexed { index, report ->
                                    val xOffset = (index * 85 + 40) % 280
                                    val yOffset = (index * 110 + 60) % 400

                                    val pinColor = when (report.status.lowercase()) {
                                        "verified" -> DangerRed
                                        "resolved" -> LeafGreen
                                        else -> AlertAmber
                                    }

                                    Surface(
                                        shape = CircleShape,
                                        color = pinColor,
                                        modifier = Modifier
                                            .padding(start = xOffset.dp, top = yOffset.dp)
                                            .size(38.dp)
                                            .border(2.dp, Color.White, CircleShape)
                                            .clickable { selectedReport = report }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Filled.Warning,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Floating Hazard Details Popup Card at bottom (Matching Wireframe 3.1)
            selectedReport?.let { report ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Hazard: ${report.category.ifBlank { "Campus Incident" }}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = InkDark
                            )
                            Surface(
                                color = AlertAmber.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = report.status.uppercase(),
                                    color = InkDark,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Location: ${report.locationLabel.ifBlank { "UM Campus" }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedText
                        )

                        if (report.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = report.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkDark
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // "Explain with Assistant" Button matching wireframe 3.1
                        Button(
                            onClick = { onNavigateToAssistant(report.description) },
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = TealTint),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        ) {
                            Text(
                                text = "Explain with Assistant",
                                color = PrimaryTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}