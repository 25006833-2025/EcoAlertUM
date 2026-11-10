package com.ecoalertum.android.learn.quiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SaveStatus { IDLE, SAVING, SAVED, NOT_SIGNED_IN, FAILED }

sealed interface QuizUiState {
    data object NotFound : QuizUiState
    data class InProgress(val session: QuizSession, val phase: QuizPhase, val language: String) : QuizUiState
    data class Finished(
        val session: QuizSession,
        val phase: QuizPhase,
        val language: String,
        val saveStatus: SaveStatus,
        /** Latest pre-quiz and post-quiz scores for this quiz (null = not taken yet). */
        val preScore: Int? = null,
        val postScore: Int? = null,
    ) : QuizUiState
}

/** Runs one quiz (FR5.1) and saves the score when it ends. */
class QuizViewModel(
    savedStateHandle: SavedStateHandle,
    quizRepository: QuizRepository,
    private val attemptRepository: QuizAttemptRepository,
    private val scoreStore: QuizScoreStore,
    private val language: String = Locale.getDefault().language,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val quiz: Quiz? = savedStateHandle.get<String>(ARG_QUIZ_ID)?.let(quizRepository::getQuiz)
    private val phase: QuizPhase = QuizPhase.fromValue(savedStateHandle.get<String>(ARG_PHASE))
    private var startedAt = nowMillis()

    private val _uiState = MutableStateFlow<QuizUiState>(
        quiz?.let { QuizUiState.InProgress(QuizSession(it), phase, language) } ?: QuizUiState.NotFound
    )
    val uiState: StateFlow<QuizUiState> = _uiState.asStateFlow()

    fun select(index: Int) = updateSession { it.select(index) }
    fun check() = updateSession { it.check() }

    fun next() {
        updateSession { it.next() }
        val state = _uiState.value
        if (state is QuizUiState.InProgress && state.session.isFinished) {
            val quizId = state.session.quiz.id
            scoreStore.record(attemptFor(state.session))
            _uiState.value = QuizUiState.Finished(
                session = state.session,
                phase = phase,
                language = language,
                saveStatus = SaveStatus.IDLE,
                preScore = scoreStore.lastScore(quizId, QuizPhase.PRE),
                postScore = scoreStore.lastScore(quizId, QuizPhase.POST),
            )
            save()
        }
    }

    /** Saves the finished attempt. Also used by the "Try again" button when saving failed. */
    fun save() {
        val state = _uiState.value as? QuizUiState.Finished ?: return
        if (state.saveStatus == SaveStatus.SAVING || state.saveStatus == SaveStatus.SAVED) return
        _uiState.value = state.copy(saveStatus = SaveStatus.SAVING)
        val attempt = attemptFor(state.session)
        viewModelScope.launch {
            val result = attemptRepository.save(attempt)
            val status = when {
                result.isSuccess -> SaveStatus.SAVED
                result.exceptionOrNull() is NotSignedInException -> SaveStatus.NOT_SIGNED_IN
                else -> SaveStatus.FAILED
            }
            _uiState.update { (it as? QuizUiState.Finished)?.copy(saveStatus = status) ?: it }
        }
    }

    /** Starts the same quiz again from question 1. */
    fun restart() {
        val q = quiz ?: return
        startedAt = nowMillis()
        _uiState.value = QuizUiState.InProgress(QuizSession(q), phase, language)
    }

    private fun attemptFor(session: QuizSession) = QuizAttempt(
        quizId = session.quiz.id,
        phase = phase,
        score = session.correctCount,
        maxScore = session.totalQuestions,
        language = language,
        durationSeconds = ((nowMillis() - startedAt) / 1000).toInt(),
    )

    private fun updateSession(change: (QuizSession) -> QuizSession) {
        _uiState.update { state ->
            if (state is QuizUiState.InProgress) state.copy(session = change(state.session)) else state
        }
    }

    companion object {
        const val ARG_QUIZ_ID = "quizId"
        const val ARG_PHASE = "phase"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                QuizViewModel(
                    createSavedStateHandle(),
                    container.quizRepository,
                    container.quizAttemptRepository,
                    container.quizScoreStore,
                )
            }
        }
    }
}
