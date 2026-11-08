package com.ecoalertum.android.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.HazardRepository
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/** Combines the three hazard flows and the alerts into one [HomeUiState]. */
class HomeViewModel(
    private val hazardRepository: HazardRepository,
    private val alertRepository: AlertRepository,
    private val nowMillis: () -> Long = System::currentTimeMillis,
    private val language: String = Locale.getDefault().language,
) : ViewModel() {

    private val retries = MutableStateFlow(0)

    @OptIn(ExperimentalCoroutinesApi::class) // flatMapLatest
    val uiState: StateFlow<HomeUiState> = retries
        .flatMapLatest {
            combine(
                hazardRepository.observeReadings(HazardType.RAIN),
                hazardRepository.observeReadings(HazardType.FLOOD),
                hazardRepository.observeReadings(HazardType.HAZE),
                alertRepository.observeAlerts(),
            ) { rain, flood, haze, alerts ->
                HomeUiStateMapper.map(rain, flood, haze, alerts, nowMillis(), language)
            }
        }
        // Keep showing the last content while a retry starts again from Loading (NFR1).
        .scan<HomeUiState, HomeUiState>(HomeUiState.Loading) { previous, next ->
            if (next is HomeUiState.Loading && previous is HomeUiState.Content) previous else next
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState.Loading)

    /** Subscribes to the repositories again. */
    fun retry() {
        retries.update { it + 1 }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                HomeViewModel(container.hazardRepository, container.alertRepository)
            }
        }
    }
}
