package com.ecoalertum.android.alerts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.DataState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Finds one alert by the ID in the route (alert/{alertId}). */
class AlertDetailViewModel(
    savedStateHandle: SavedStateHandle,
    alertRepository: AlertRepository,
) : ViewModel() {

    private val alertId: String = checkNotNull(savedStateHandle["alertId"])

    val alert: StateFlow<DataState<Alert?>> = alertRepository.observeAlerts()
        .map { it.pick(alertId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataState.Loading)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                AlertDetailViewModel(createSavedStateHandle(), container.alertRepository)
            }
        }
    }
}

/** Picks the alert with [id] out of the list, keeping Loading / Success / Error. */
private fun DataState<List<Alert>>.pick(id: String): DataState<Alert?> = when (this) {
    DataState.Loading -> DataState.Loading
    is DataState.Success -> DataState.Success(data.firstOrNull { it.id == id }, isOutdated, lastUpdated)
    is DataState.Error -> DataState.Error(message, cachedData?.firstOrNull { it.id == id }, lastUpdated)
}