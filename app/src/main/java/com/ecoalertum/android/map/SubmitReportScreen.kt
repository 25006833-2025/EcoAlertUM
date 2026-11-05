package com.ecoalertum.android.map

import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ecoalertum.android.R
import com.ecoalertum.android.ui.theme.EcoAlertUMTheme
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

private const val MAX_DESCRIPTION = 500

/** Design system: corner radius 14, spacing on an 8 dp grid, touch targets at least 48 dp. */
private val CornerRadius = 14.dp

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
    var menuOpen by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val canSubmit = category != null && description.isNotBlank() && !isSaving

    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Evidence (the photo is added in a later step)
        SectionLabel(R.string.reports_evidence)
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            shape = RoundedCornerShape(CornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
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

        // Location (GPS is added in a later step)
        SectionLabel(R.string.reports_location)
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(CornerRadius),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(R.string.reports_location_pending),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

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

        // Submit: saves to Firestore, then goes back
        Button(
            onClick = {
                category?.let { selected ->
                    scope.launch {
                        isSaving = true
                        saveFailed = false
                        val report = HazardReport(
                            reporterUid = currentUid(),
                            category = selected.key,
                            description = description.trim(),
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