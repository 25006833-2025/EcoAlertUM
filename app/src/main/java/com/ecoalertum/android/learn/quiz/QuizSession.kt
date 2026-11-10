package com.ecoalertum.android.learn.quiz

/**
 * The rules of one quiz run, with no Android code so it can be unit tested.
 * Flow per question: [select] an option, [check] it, then [next]. Each step returns a new session.
 */
data class QuizSession(
    val quiz: Quiz,
    val currentIndex: Int = 0,
    val selectedIndex: Int? = null,
    val isChecked: Boolean = false,
    /** The option picked for each checked question, in order. */
    val answers: List<Int> = emptyList(),
    val isFinished: Boolean = false,
) {
    val currentQuestion: QuizQuestion get() = quiz.questions[currentIndex]
    val totalQuestions: Int get() = quiz.questions.size
    val isLastQuestion: Boolean get() = currentIndex == quiz.questions.lastIndex

    val correctCount: Int
        get() = answers.withIndex().count { (i, picked) -> quiz.questions[i].correctIndex == picked }
    val wrongCount: Int get() = answers.size - correctCount

    /** True when the checked answer for the current question is right. */
    val isCurrentCorrect: Boolean
        get() = isChecked && selectedIndex == currentQuestion.correctIndex

    /** Picks an option. Ignored once the answer has been checked. */
    fun select(index: Int): QuizSession =
        if (isChecked || isFinished || index !in currentQuestion.options.indices) this
        else copy(selectedIndex = index)

    /** Locks in the selected option and counts it. Needs a selection. */
    fun check(): QuizSession {
        val picked = selectedIndex ?: return this
        if (isChecked || isFinished) return this
        return copy(isChecked = true, answers = answers + picked)
    }

    /** Moves to the next question, or finishes after the last one. */
    fun next(): QuizSession = when {
        !isChecked || isFinished -> this
        isLastQuestion -> copy(isFinished = true)
        else -> copy(currentIndex = currentIndex + 1, selectedIndex = null, isChecked = false)
    }
}
