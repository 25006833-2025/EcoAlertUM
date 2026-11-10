package com.ecoalertum.android.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.HazardType
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.learn.quiz.Quiz
import com.ecoalertum.android.learn.quiz.QuizRepository
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class LearnUiState(
    /** All quizzes. The one for the active hazard comes first, so the "Right now" card shows it. */
    val quizzes: List<Quiz>,
    /** Topic of the most severe active alert, e.g. `floods`, or null when nothing is active. */
    val activeTopic: String? = null,
    /** That alert's title, shown as "RIGHT NOW · …". */
    val activeLabel: String? = null,
    val language: String = "en",
)

/** Learn tab: the quiz list plus which topic matches the current hazard alert. */
class LearnViewModel(
    quizRepository: QuizRepository,
    alertRepository: AlertRepository,
    private val language: String = Locale.getDefault().language,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val quizzes = quizRepository.getQuizzes()

    val uiState: StateFlow<LearnUiState> = alertRepository.observeAlerts()
        .map { state -> build(state.alertsOrEmpty()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LearnUiState(quizzes, language = language))

    private fun build(alerts: List<Alert>): LearnUiState {
        val now = nowMillis()
        val active = alerts
            .filter { it.status == AlertStatus.ACTIVE && (it.expiresAt == 0L || it.expiresAt > now) }
            .maxByOrNull { it.level }
        val topic = active?.let { topicFor(it.hazard) }
        val ordered = if (topic == null) quizzes else quizzes.sortedByDescending { it.topic == topic }
        return LearnUiState(
            quizzes = ordered,
            activeTopic = topic,
            activeLabel = active?.title?.get(language)?.takeIf { it.isNotBlank() },
            language = language,
        )
    }

    private fun DataState<List<Alert>>.alertsOrEmpty(): List<Alert> = when (this) {
        is DataState.Success -> data
        is DataState.Error -> cachedData.orEmpty()
        DataState.Loading -> emptyList()
    }

    companion object {
        /** Heavy rain and river level both lead to the flood lessons. */
        fun topicFor(hazard: HazardType): String = when (hazard) {
            HazardType.RAIN, HazardType.FLOOD -> LearnTopic.FLOODS.id
            HazardType.HAZE -> LearnTopic.HAZE.id
        }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                LearnViewModel(container.quizRepository, container.alertRepository)
            }
        }
    }
}
