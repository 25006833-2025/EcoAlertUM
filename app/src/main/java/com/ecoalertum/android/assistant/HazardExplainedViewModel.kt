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
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.ecoalertum.android.R


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
    application: Application,
    private val repository: AssistantRepository = AssistantRepository(),
    private val mockRepository: MockAssistantRepository = MockAssistantRepository(),
) : AndroidViewModel(application) {

    private val _uiState =
        MutableStateFlow<HazardExplanationUiState>(
            HazardExplanationUiState.Idle
        )

    val uiState: StateFlow<HazardExplanationUiState> =
        _uiState.asStateFlow()

    fun explainHazard(alertId: String) {
        if (alertId.isBlank()) {
            _uiState.value = HazardExplanationUiState.Error(
                getApplication<Application>().getString(R.string.assistant_error_no_alert)
            )
            return
        }

        if (_uiState.value is HazardExplanationUiState.Loading) {
            return
        }

        viewModelScope.launch {
            _uiState.value = HazardExplanationUiState.Loading

            try {
                val response = if (BuildConfig.DEBUG && BuildConfig.USE_MOCK_AI) {
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
                    getApplication<Application>().getString(R.string.assistant_error_retrieve)
                )
            }
        }
    }
}