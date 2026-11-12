package com.ecoalertum.android.assistant

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ecoalertum.android.alerts.AlertDetailViewModel
import com.ecoalertum.android.core.data.repository.DataState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Composable
fun HazardExplainedScreen(
    alertId: String,
    modifier: Modifier = Modifier,
    viewModel: HazardExplainedViewModel = viewModel(),
    alertViewModel: AlertDetailViewModel = viewModel(
        factory = AlertDetailViewModel.Factory
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val alertState by alertViewModel.alert.collectAsStateWithLifecycle()

    LaunchedEffect(alertId) {
        viewModel.explainHazard(alertId)
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Scrollable content
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Hazard explained",
                style = MaterialTheme.typography.headlineSmall,
            )

            Card(Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Selected hazard",
                        style = MaterialTheme.typography.titleMedium,
                    )

                    val selectedAlert = when (val state = alertState) {
                        is DataState.Success -> state.data
                        is DataState.Error -> state.cachedData
                        DataState.Loading -> null
                    }

                    if (selectedAlert != null) {
                        Text(
                            text = selectedAlert.title.get("en"),
                            style = MaterialTheme.typography.titleLarge,
                        )

                        Text(
                            "Severity: ${selectedAlert.level.value.uppercase()}"
                        )
                        Text("Area: ${selectedAlert.area}")

                        Text(
                            "Issued: ${formatAlertTime(selectedAlert.issuedAt)}"
                        )

                        Text(
                            "Expires: ${formatAlertTime(selectedAlert.expiresAt)}"
                        )

                        Text(
                            text = selectedAlert.message.get("en"),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        when (alertState) {
                            DataState.Loading -> {
                                CircularProgressIndicator()
                                Text("Loading alert information...")
                            }

                            else -> {
                                Text("Alert information unavailable.")
                                Text("Alert ID: $alertId")
                            }
                        }
                    }
                }
            }

            Text(
                text = "In plain language (AI)",
                style = MaterialTheme.typography.titleMedium,
            )

            when (val state = uiState) {
                HazardExplanationUiState.Idle -> {
                    Text("Preparing hazard explanation...")
                }

                HazardExplanationUiState.Loading -> {
                    CircularProgressIndicator()
                    Text("Generating hazard explanation...")
                }

                is HazardExplanationUiState.Success -> {
                    val response = state.response

                    if (response.status == "fallback") {
                        Text(
                            text = "AI is temporarily unavailable. " +
                                    "Showing predefined safety guidance.",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Text(
                        text = response.explanation,
                        style = MaterialTheme.typography.bodyLarge,
                    )

                    if (response.steps.isNotEmpty()) {
                        Text(
                            text = "What to do now",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        response.steps.forEachIndexed { index, step ->
                            Text("${index + 1}. $step")
                        }
                    }

                    if (response.sources.isNotEmpty()) {
                        Text(
                            text = "Sources",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        response.sources.forEach { source ->
                            Text(source)
                        }
                    }
                }

                is HazardExplanationUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                    )

                    Button(
                        onClick = {
                            viewModel.explainHazard(alertId)
                        },
                    ) {
                        Text("Try again")
                    }
                }
            }
        }

        // Fixed action buttons above the existing bottom navigation
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.weight(1f),
            ) {
                Text("Ask a follow-up")
            }

            OutlinedButton(
                onClick = {},
                enabled = false,
                modifier = Modifier.weight(1f),
            ) {
                Text("Related lesson")
            }
        }
    }
}

private fun formatAlertTime(timestamp: Long): String {
    if (timestamp <= 0L) return "Unknown"

    val formatter = SimpleDateFormat(
        "dd MMM yyyy, hh:mm a",
        Locale.ENGLISH
    ).apply {
        timeZone = TimeZone.getTimeZone("Asia/Kuala_Lumpur")
    }

    return formatter.format(Date(timestamp))
}
