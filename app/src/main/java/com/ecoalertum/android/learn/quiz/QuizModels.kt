package com.ecoalertum.android.learn.quiz

import com.ecoalertum.android.core.data.model.LocalizedText

/**
 * Why the student is taking the quiz (stored as `phase` in `quizAttempts`).
 * PRE and POST scores are compared to measure learning gain (FR5.3).
 */
enum class QuizPhase(val value: String) {
    PRE("pre"),
    POST("post"),
    PRACTICE("practice");

    companion object {
        fun fromValue(value: String?): QuizPhase = entries.firstOrNull { it.value == value } ?: PRACTICE
    }
}

/** One quiz, bundled in `assets/quizzes/quizzes.json` for v1 (see docs/data-models.md). */
data class Quiz(
    val id: String,
    /** Hazard topic: `floods`, `haze` or `storms`. Used to link lessons and badges later. */
    val topic: String,
    val title: LocalizedText,
    val questions: List<QuizQuestion>,
)

data class QuizQuestion(
    val id: String,
    val prompt: LocalizedText,
    val options: List<LocalizedText>,
    /** Index into [options] of the right answer. */
    val correctIndex: Int,
    /** Shown after the student checks their answer. */
    val explanation: LocalizedText,
)

/** One finished attempt, saved to the `quizAttempts` collection. */
data class QuizAttempt(
    val quizId: String,
    val phase: QuizPhase,
    val score: Int,
    val maxScore: Int,
    val language: String,
    val durationSeconds: Int,
)
