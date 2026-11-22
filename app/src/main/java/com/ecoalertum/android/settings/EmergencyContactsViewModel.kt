package com.ecoalertum.android.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.data.model.EmergencyContact
import com.ecoalertum.android.core.data.repository.EmergencyContactRepository
import java.util.Locale

/** Holds the emergency contacts for the screen (FR6.2). The list is bundled, so there is no loading state. */
class EmergencyContactsViewModel(
    repository: EmergencyContactRepository,
    /** "en" or "ms". Names fall back to English when there is no Malay text. */
    val language: String = Locale.getDefault().language,
) : ViewModel() {

    val contacts: List<EmergencyContact> = repository.getContacts()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                EmergencyContactsViewModel(container.emergencyContactRepository)
            }
        }
    }
}
