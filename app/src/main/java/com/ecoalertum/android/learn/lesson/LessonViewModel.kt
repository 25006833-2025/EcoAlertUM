package com.ecoalertum.android.learn.lesson

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.learn.quiz.QuizRepository
import java.util.Locale

/** One lesson opened from the Learn tab or a hazard screen (FR5.2). */
class LessonViewModel(
    savedStateHandle: SavedStateHandle,
    lessonRepository: LessonRepository,
    quizRepository: QuizRepository,
    private val progressStore: LessonProgressStore,
    val language: String = Locale.getDefault().language,
) : ViewModel() {

    private val topic: String = savedStateHandle.get<String>(ARG_TOPIC).orEmpty()

    /** Null when no lesson exists for this topic. */
    val lesson: Lesson? = lessonRepository.forTopic(topic)

    /** The topic's quiz, for the "Take the post-quiz" button on the last card. */
    val quizId: String? = quizRepository.getQuizzes().firstOrNull { it.topic == topic }?.id

    /** Called when the student reaches the last card. */
    fun markCompleted() {
        lesson?.let { progressStore.markCompleted(it.id) }
    }

    companion object {
        const val ARG_TOPIC = "topic"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                LessonViewModel(
                    createSavedStateHandle(),
                    container.lessonRepository,
                    container.quizRepository,
                    container.lessonProgressStore,
                )
            }
        }
    }
}
