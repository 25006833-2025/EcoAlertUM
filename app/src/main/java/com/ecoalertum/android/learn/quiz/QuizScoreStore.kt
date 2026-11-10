package com.ecoalertum.android.learn.quiz

import android.content.Context

/**
 * The student's latest score for each quiz and phase, kept on the phone so the result screen can
 * always show "before vs after", even offline or before login. Firestore (`quizAttempts`) stays the
 * record used for the SDG 13.3 evaluation.
 */
interface QuizScoreStore {
    fun lastScore(quizId: String, phase: QuizPhase): Int?
    fun record(attempt: QuizAttempt)
}

class SharedPrefsQuizScoreStore(context: Context) : QuizScoreStore {
    private val prefs = context.applicationContext.getSharedPreferences("quiz_scores", Context.MODE_PRIVATE)

    override fun lastScore(quizId: String, phase: QuizPhase): Int? =
        prefs.getInt(key(quizId, phase), -1).takeIf { it >= 0 }

    override fun record(attempt: QuizAttempt) {
        prefs.edit().putInt(key(attempt.quizId, attempt.phase), attempt.score).apply()
    }

    private fun key(quizId: String, phase: QuizPhase) = "$quizId.${phase.value}"
}
