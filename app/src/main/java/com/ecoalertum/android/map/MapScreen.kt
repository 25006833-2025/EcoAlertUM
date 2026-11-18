package com.ecoalertum.android.map

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.ecoalertum.android.R
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

// Official Design System Tokens (from EcoAlert UM design system)
private val PrimaryTeal = Color(0xFF0F4C47)
private val TealTint = Color(0xFFE3F0EC)
private val LeafGreen = Color(0xFF1F7A4A)
private val AlertAmber = Color(0xFFF2A23A)
private val DangerRed = Color(0xFF9F1D14)
private val InkDark = Color(0xFF10201E)
private val MutedText = Color(0xFF4A5B58)
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

    // Pusat Universiti Malaya sebagai default camera position
    val umCampus = LatLng(3.1236, 101.6547)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(umCampus, 15f)
    }

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
            // Real Google Maps View
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(zoomControlsEnabled = true)
            ) {
                displayedReports.forEach { report ->
                    // Ambil koordinat tepat dari Firebase GeoPoint model
                    report.location?.let { geoPoint ->
                        val position = LatLng(geoPoint.latitude, geoPoint.longitude)

                        Marker(
                            state = MarkerState(position = position),
                            title = report.category.ifBlank { "Campus Incident" },
                            snippet = report.locationLabel,
                            // Guna warna standard mengikut status supaya peta stabil & tak crash
                            icon = BitmapDescriptorFactory.defaultMarker(
                                when (report.status.lowercase()) {
                                    "verified" -> BitmapDescriptorFactory.HUE_RED
                                    "resolved" -> BitmapDescriptorFactory.HUE_GREEN
                                    else -> BitmapDescriptorFactory.HUE_ORANGE
                                }
                            ),
                            onClick = {
                                selectedReport = report
                                false
                            }
                        )
                    }
                }
            }

            // Top Filter Chips Overlay
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
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
            }

            // Loading / Error States overlay
            if (isLoading) {
                CircularProgressIndicator(
                    color = PrimaryTeal,
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            // Floating Hazard Details Popup Card at bottom
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