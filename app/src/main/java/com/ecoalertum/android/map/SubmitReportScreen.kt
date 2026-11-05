package com.ecoalertum.android.map

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.ecoalertum.android.R
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_DESCRIPTION = 500
private const val PREVIEW_MAX_SIDE = 800

/** Design system: corner radius 14, spacing on an 8 dp grid, touch targets at least 48 dp. */
private val CornerRadius = 14.dp

private val LOCATION_PERMISSIONS = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION,
)

private enum class LocationStatus { Loading, Found, Unavailable, Denied }

private fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

/** Signed-in user's id. Falls back to a test id until login (FR6.1) is merged. */
fun currentUid(): String =
    FirebaseAuth.getInstance().currentUser?.uid ?: "test-user"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubmitReportScreen(
    modifier: Modifier = Modifier,
    onSubmitted: () -> Unit = {},
) {
    var category by rememberSaveable { mutableStateOf<ReportCategory?>(null) }
    var description by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var placeName by rememberSaveable { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var preview by remember { mutableStateOf<ImageBitmap?>(null) }
    var geoPoint by remember { mutableStateOf<GeoPoint?>(null) }
    var locationStatus by remember { mutableStateOf(LocationStatus.Loading) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun findLocation() {
        scope.launch {
            locationStatus = LocationStatus.Loading
            val found = readCurrentLocation(context)
            if (found == null) {
                locationStatus = LocationStatus.Unavailable
            } else {
                geoPoint = GeoPoint(found.latitude, found.longitude)
                locationStatus = LocationStatus.Found
                if (placeName.isBlank()) {
                    placeName = lookUpPlaceName(context, found.latitude, found.longitude)
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        if (results.values.any { it }) findLocation() else locationStatus = LocationStatus.Denied
    }

    val pickPhoto = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) photoUri = uri
    }

    // Ask for location permission (or use it if already granted) when the screen opens.
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            findLocation()
        } else {
            permissionLauncher.launch(LOCATION_PERMISSIONS)
        }
    }

    // Load a small preview whenever a photo is chosen.
    LaunchedEffect(photoUri) {
        val uri = photoUri
        preview = if (uri == null) {
            null
        } else {
            withContext(Dispatchers.IO) {
                decodeScaledBitmap(context, uri, PREVIEW_MAX_SIDE)?.asImageBitmap()
            }
        }
    }

    val canSubmit = category != null &&
            description.isNotBlank() &&
            geoPoint != null &&
            !isSaving

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Evidence: tap to choose a photo from the gallery
        SectionLabel(R.string.reports_evidence)
        Surface(
            onClick = {
                pickPhoto.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
            enabled = !isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(CornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            val shown = preview
            if (shown != null) {
                Image(
                    bitmap = shown,
                    contentDescription = stringResource(R.string.reports_photo_preview),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AddCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        text = stringResource(R.string.reports_add_photo),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        if (photoUri != null) {
            Text(
                text = stringResource(R.string.reports_photo_change),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Category
        SectionLabel(R.string.reports_category)
        ExposedDropdownMenuBox(
            expanded = menuOpen,
            onExpandedChange = { menuOpen = it },
        ) {
            OutlinedTextField(
                value = category?.let { stringResource(it.labelRes) } ?: "",
                onValueChange = {},
                readOnly = true,
                placeholder = { Text(stringResource(R.string.reports_category_hint)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuOpen) },
                shape = RoundedCornerShape(CornerRadius),
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
            ) {
                ReportCategory.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes)) },
                        onClick = {
                            category = option
                            menuOpen = false
                        },
                    )
                }
            }
        }

        // Location: GPS position, plus an optional place name
        SectionLabel(R.string.reports_location)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (locationStatus == LocationStatus.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                    val point = geoPoint
                    Text(
                        text = when (locationStatus) {
                            LocationStatus.Loading ->
                                stringResource(R.string.reports_location_loading)
                            LocationStatus.Found ->
                                if (point != null) {
                                    stringResource(
                                        R.string.reports_location_found,
                                        point.latitude,
                                        point.longitude,
                                    )
                                } else {
                                    ""
                                }
                            LocationStatus.Unavailable ->
                                stringResource(R.string.reports_location_unavailable)
                            LocationStatus.Denied ->
                                stringResource(R.string.reports_location_denied)
                        },
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
                if (locationStatus == LocationStatus.Unavailable ||
                    locationStatus == LocationStatus.Denied
                ) {
                    TextButton(
                        onClick = {
                            if (hasLocationPermission(context)) {
                                findLocation()
                            } else {
                                permissionLauncher.launch(LOCATION_PERMISSIONS)
                            }
                        },
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.reports_location_retry))
                    }
                }
            }
        }
        OutlinedTextField(
            value = placeName,
            onValueChange = { placeName = it },
            label = { Text(stringResource(R.string.reports_place_name)) },
            placeholder = { Text(stringResource(R.string.reports_place_name_hint)) },
            singleLine = true,
            shape = RoundedCornerShape(CornerRadius),
            modifier = Modifier.fillMaxWidth(),
        )

        // Description
        SectionLabel(R.string.reports_description)
        OutlinedTextField(
            value = description,
            onValueChange = { if (it.length <= MAX_DESCRIPTION) description = it },
            minLines = 4,
            maxLines = 8,
            shape = RoundedCornerShape(CornerRadius),
            supportingText = { Text("${description.length}/$MAX_DESCRIPTION") },
            modifier = Modifier.fillMaxWidth(),
        )

        // Submit: uploads the photo (if any), saves the report, then goes back
        Button(
            onClick = {
                category?.let { selected ->
                    scope.launch {
                        isSaving = true
                        saveFailed = false
                        val uid = currentUid()

                        var photoPath: String? = null
                        val uri = photoUri
                        if (uri != null) {
                            val upload = uploadReportPhoto(context, uri, uid)
                            if (upload.isFailure) {
                                saveFailed = true
                                isSaving = false
                                return@launch
                            }
                            photoPath = upload.getOrNull()
                        }

                        val report = HazardReport(
                            reporterUid = uid,
                            category = selected.key,
                            description = description.trim(),
                            photoPath = photoPath,
                            location = geoPoint,
                            locationLabel = placeName.trim(),
                            status = "unverified",
                        )
                        HazardReportRepository().submitReport(report)
                            .onSuccess {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.reports_saved),
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onSubmitted()
                            }
                            .onFailure { saveFailed = true }
                        isSaving = false
                    }
                }
            },
            enabled = canSubmit,
            shape = RoundedCornerShape(CornerRadius),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
        ) {
            if (isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(stringResource(R.string.reports_submit))
            }
        }

        if (saveFailed) {
            Text(
                text = stringResource(R.string.reports_save_failed),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun SectionLabel(@StringRes textRes: Int) {
    Text(
        text = stringResource(textRes),
        style = MaterialTheme.typography.titleMedium,
    )
}

@Preview(showBackground = true)
@Composable
private fun SubmitReportScreenPreview() {
    EcoAlertUMTheme {
        SubmitReportScreen()
    }
}