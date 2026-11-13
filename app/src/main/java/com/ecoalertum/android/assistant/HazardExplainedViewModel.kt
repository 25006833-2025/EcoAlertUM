package com.ecoalertum.android.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log
import com.ecoalertum.android.BuildConfig

sealed interface HazardExplanationUiState {
    data object Idle : HazardExplanationUiState
    data object Loading : HazardExplanationUiState

    data class Success(
        val response: AssistantResponse,
    ) : HazardExplanationUiState

    data class Error(
        val message: String,
    ) : HazardExplanationUiState
}

class HazardExplainedViewModel(
    private val repository: AssistantRepository = AssistantRepository(),
    private val mockRepository: MockAssistantRepository = MockAssistantRepository(),
) : ViewModel() {

    private val _uiState =
        MutableStateFlow<HazardExplanationUiState>(
            HazardExplanationUiState.Idle
        )

    val uiState: StateFlow<HazardExplanationUiState> =
        _uiState.asStateFlow()

    fun explainHazard(alertId: String) {
        if (alertId.isBlank()) {
            _uiState.value = HazardExplanationUiState.Error(
                "No alert was selected."
            )
            return
        }

        if (_uiState.value is HazardExplanationUiState.Loading) {
            return
        }

        viewModelScope.launch {
            _uiState.value = HazardExplanationUiState.Loading

            try {
                val response = if (BuildConfig.DEBUG && USE_MOCK_AI) {
                    mockRepository.explainHazard(
                        alertId = alertId,
                        language = "en"
                    )
                } else {
                    repository.explainHazard(
                        alertId = alertId,
                        language = "en"
                    )
                }

                _uiState.value =
                    HazardExplanationUiState.Success(response)

            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(
                    "HazardExplained",
                    "Failed to retrieve AI hazard explanation",
                    error
                )

                _uiState.value = HazardExplanationUiState.Error(
                    "Unable to retrieve the hazard explanation. " +
                            "Please try again later."
                )
            }
        }
    }
}

// Development-only setting.
// Set to false when testing the Firebase/Gemini integration.
private const val USE_MOCK_AI = false