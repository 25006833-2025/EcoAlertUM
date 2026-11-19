package com.ecoalertum.android.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ecoalertum.android.R
import com.google.android.gms.maps.model.BitmapDescriptor
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
private val AlertAmber = Color(0xFFF2A23A)
private val InkDark = Color(0xFF10201E)
private val MutedText = Color(0xFF4A5B58)
private val ScreenBg = Color(0xFFF4F7F5)

private val MarkerVerifiedBg = Color(0xFFC62828)
private val MarkerResolvedBg = Color(0xFF1F7A4A)
private val MarkerPendingBg = Color(0xFFD97706)

private const val FILTER_ALL = "All"
private const val FILTER_MY_REPORTS = "My Reports"
private const val FILTER_OTHER = "Other"

private fun matchedPresetCategory(rawCategory: String): ReportCategory? {
    val trimmed = rawCategory.trim()
    if (trimmed.isEmpty()) return null
    return ReportCategory.entries.firstOrNull { !it.isCustom && it.key == trimmed }
}

private fun formatMapCategoryLabel(
    rawCategory: String,
    resolveString: (Int) -> String,
): String {
    val trimmed = rawCategory.trim()
    if (trimmed.isEmpty()) return "Campus Incident"
    val matched = matchedPresetCategory(trimmed)
    if (matched?.labelRes != null) {
        return resolveString(matched.labelRes)
    }
    return trimmed.replace('_', ' ').replaceFirstChar { it.uppercaseChar() }
}

private fun mapFilterBucket(
    report: HazardReport,
    resolveString: (Int) -> String,
): String {
    val matched = matchedPresetCategory(report.category)
    return if (matched?.labelRes != null) {
        resolveString(matched.labelRes)
    } else {
        FILTER_OTHER
    }
}

private fun isActiveMapReport(status: String): Boolean {
    val normalized = status.trim().lowercase()
    return normalized == "unverified" || normalized == "verified"
}

@DrawableRes
private fun resolveMapHazardDrawableRes(categoryKey: String): Int {
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

private fun markerBadgeColor(status: String): Color {
    return when (status.trim().lowercase()) {
        "verified" -> MarkerVerifiedBg
        "resolved" -> MarkerResolvedBg
        else -> MarkerPendingBg
    }
}

private fun createHazardMarkerDescriptor(
    context: Context,
    @DrawableRes iconRes: Int,
    badgeColor: Color,
): BitmapDescriptor {
    val density = context.resources.displayMetrics.density
    val sizePx = (44 * density).toInt().coerceAtLeast(48)
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)

    val center = sizePx / 2f
    val outerRadius = sizePx * 0.45f
    val borderWidth = sizePx * 0.06f

    // Bingkai putih luar supaya jelas di atas peta
    val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, outerRadius, borderPaint)

    // Latar bulatan warna ikut status laporan
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = badgeColor.toArgb()
        style = Paint.Style.FILL
    }
    canvas.drawCircle(center, center, outerRadius - borderWidth, badgePaint)

    // Lukis ikon hazard putih di tengah
    val drawable = ContextCompat.getDrawable(context, iconRes)?.mutate()
    if (drawable != null) {
        drawable.colorFilter = PorterDuffColorFilter(
            android.graphics.Color.WHITE,
            PorterDuff.Mode.SRC_IN,
        )
        val iconInset = (sizePx * 0.25f).toInt()
        drawable.setBounds(
            iconInset,
            iconInset,
            sizePx - iconInset,
            sizePx - iconInset,
        )
        drawable.draw(canvas)
    }

    return BitmapDescriptorFactory.fromBitmap(bitmap)
}

@Composable
fun MapScreen(
    modifier: Modifier = Modifier,
    repository: HazardReportRepository = remember { HazardReportRepository() },
    onNavigateToAssistant: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val uid = remember { currentUid() }

    var reports by remember { mutableStateOf<List<HazardReport>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedFilter by remember { mutableStateOf(FILTER_ALL) }
    var selectedReport by remember { mutableStateOf<HazardReport?>(null) }

    val markerDescriptorCache = remember(context) {
        mutableMapOf<Pair<Int, Int>, BitmapDescriptor>()
    }

    fun getHazardMarker(categoryKey: String, status: String): BitmapDescriptor {
        val iconRes = resolveMapHazardDrawableRes(categoryKey)
        val bgArgb = markerBadgeColor(status).toArgb()
        val cacheKey = iconRes to bgArgb
        return markerDescriptorCache.getOrPut(cacheKey) {
            createHazardMarkerDescriptor(
                context = context,
                iconRes = iconRes,
                badgeColor = Color(bgArgb),
            )
        }
    }

    // Pusat Universiti Malaya sebagai default camera position
    val umCampus = LatLng(3.1236, 101.6547)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(umCampus, 15f)
    }

    DisposableEffect(repository) {
        val registration = repository.getReportsStream(
            onSuccess = { fetched ->
                reports = fetched
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

    val verifiedReports = remember(reports) {
        reports.filter { it.status.equals("verified", ignoreCase = true) }
    }

    val availableCategoryChips = remember(verifiedReports) {
        val buckets = verifiedReports
            .map { report -> mapFilterBucket(report) { resId -> context.getString(resId) } }
            .toSet()

        val presetLabels = ReportCategory.entries
            .filter { !it.isCustom && it.labelRes != null }
            .map { context.getString(it.labelRes!!) }
            .filter { it in buckets }

        buildList {
            addAll(presetLabels)
            if (FILTER_OTHER in buckets) add(FILTER_OTHER)
        }
    }

    val filterChips = remember(availableCategoryChips) {
        buildList {
            add(FILTER_ALL)
            add(FILTER_MY_REPORTS)
            addAll(availableCategoryChips)
        }
    }

    LaunchedEffect(filterChips) {
        if (selectedFilter !in filterChips) {
            selectedFilter = FILTER_ALL
        }
    }

    val displayedReports = remember(reports, verifiedReports, selectedFilter, uid) {
        when (selectedFilter) {
            FILTER_ALL -> verifiedReports
            FILTER_MY_REPORTS -> reports.filter {
                it.reporterUid == uid && isActiveMapReport(it.status)
            }
            else -> verifiedReports.filter { report ->
                val bucket = mapFilterBucket(report) { resId -> context.getString(resId) }
                bucket.equals(selectedFilter, ignoreCase = true)
            }
        }
    }

    LaunchedEffect(displayedReports) {
        if (displayedReports.isEmpty()) {
            selectedReport = null
        } else if (selectedReport == null || displayedReports.none { it.id == selectedReport?.id }) {
            selectedReport = displayedReports.first()
        }
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
                    report.location?.let { geoPoint ->
                        val position = LatLng(geoPoint.latitude, geoPoint.longitude)
                        val categoryTitle = formatMapCategoryLabel(report.category) { resId ->
                            context.getString(resId)
                        }

                        Marker(
                            state = MarkerState(position = position),
                            title = categoryTitle,
                            snippet = report.locationLabel,
                            icon = getHazardMarker(report.category, report.status),
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
                    items(filterChips, key = { it }) { chip ->
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
                val categoryTitle = formatMapCategoryLabel(report.category) { resId ->
                    context.getString(resId)
                }

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
                                text = "Hazard: $categoryTitle",
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