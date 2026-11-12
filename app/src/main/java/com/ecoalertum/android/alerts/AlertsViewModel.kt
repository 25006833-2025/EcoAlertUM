package com.ecoalertum.android.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.DataState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class AlertsViewModel(
    private val alertRepository: AlertRepository,
) : ViewModel() {

    val selectedType = MutableStateFlow<HazardType?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    val alerts: StateFlow<DataState<List<Alert>>> = selectedType
        .flatMapLatest { type -> alertRepository.observeAlerts(type) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataState.Loading)

    fun selectFilter(type: HazardType?) {
        selectedType.value = type
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                AlertsViewModel(container.alertRepository)
            }
        }
    }
}