package com.ecoalertum.android.learn

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.data.model.Alert
import com.ecoalertum.android.core.data.model.AlertStatus
import com.ecoalertum.android.core.data.model.ReadingType
import com.ecoalertum.android.core.data.repository.AlertRepository
import com.ecoalertum.android.core.data.repository.DataState
import com.ecoalertum.android.learn.lesson.Lesson
import com.ecoalertum.android.learn.lesson.LessonProgressStore
import com.ecoalertum.android.learn.lesson.LessonRepository
import com.ecoalertum.android.learn.quiz.Quiz
import com.ecoalertum.android.learn.quiz.QuizRepository
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class LearnUiState(
    /** All quizzes. The one for the active hazard comes first, so the "Right now" card shows it. */
    val quizzes: List<Quiz>,
    /** Topic of the most severe active alert, e.g. `floods`, or null when nothing is active. */
    val activeTopic: String? = null,
    /** That alert's title, shown as "RIGHT NOW · …". */
    val activeLabel: String? = null,
    val language: String = "en",
    /** The lesson for each topic, keyed by topic (FR5.2). */
    val lessons: Map<String, Lesson> = emptyMap(),
    /** IDs of lessons this student has finished. */
    val completedLessons: Set<String> = emptySet(),
) {
    fun isLessonDone(topic: String): Boolean = lessons[topic]?.id?.let { it in completedLessons } == true
}

/** Learn tab: the quiz list plus which topic matches the current hazard alert. */
class LearnViewModel(
    quizRepository: QuizRepository,
    alertRepository: AlertRepository,
    lessonRepository: LessonRepository,
    lessonProgressStore: LessonProgressStore,
    private val language: String = Locale.getDefault().language,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val quizzes = quizRepository.getQuizzes()
    private val lessons = lessonRepository.getLessons().associateBy { it.topic }

    val uiState: StateFlow<LearnUiState> =
        combine(alertRepository.observeAlerts(), lessonProgressStore.completed) { alerts, done ->
            build(alertsFrom(alerts)).copy(lessons = lessons, completedLessons = done)
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            LearnUiState(quizzes, language = language, lessons = lessons, completedLessons = lessonProgressStore.completed.value),
        )

    private fun build(alerts: List<Alert>): LearnUiState {
        val active = activeAlert(alerts, nowMillis())
        val topic = active?.let { topicFor(it.hazardType) }
        val ordered = if (topic == null) quizzes else quizzes.sortedByDescending { it.topic == topic }
        return LearnUiState(
            quizzes = ordered,
            activeTopic = topic,
            activeLabel = active?.title?.get(language)?.takeIf { it.isNotBlank() },
            language = language,
        )
    }

    companion object {
        /**
         * The one place that links an alert to a lesson topic (FR5.2).
         * Rain gauge and river level -> floods, Air Pollutant Index -> haze, MET weather warning -> storms.
         * Heat has no alert source yet, so its lesson is only opened from the topic list.
         */
        fun topicFor(type: ReadingType): String = when (type) {
            ReadingType.RAINFALL, ReadingType.FLOOD_LEVEL -> LearnTopic.FLOODS.id
            ReadingType.AIR_QUALITY -> LearnTopic.HAZE.id
            ReadingType.WEATHER_WARNING -> LearnTopic.STORMS.id
        }

        /**
         * The alert the Learn tab follows: the most severe one that is still active.
         * Expired, cancelled and past-expiry alerts are ignored (expiresAt = 0 means no end time).
         */
        fun activeAlert(alerts: List<Alert>, now: Long): Alert? = alerts
            .filter { it.status == AlertStatus.ACTIVE && (it.expiresAt == 0L || it.expiresAt > now) }
            .maxByOrNull { it.level }

        /** Loading shows no highlight; an error falls back to the last saved alerts, if any. */
        fun alertsFrom(state: DataState<List<Alert>>): List<Alert> = when (state) {
            is DataState.Success -> state.data
            is DataState.Error -> state.cachedData.orEmpty()
            DataState.Loading -> emptyList()
        }

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                LearnViewModel(
                    container.quizRepository,
                    container.alertRepository,
                    container.lessonRepository,
                    container.lessonProgressStore,
                )
            }
        }
    }
}
