package com.ecoalertum.android.learn.quiz

import com.ecoalertum.android.core.data.model.LocalizedText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizSessionTest {

    private fun question(correct: Int) = QuizQuestion(
        id = "q",
        prompt = LocalizedText("?"),
        options = listOf(LocalizedText("A"), LocalizedText("B"), LocalizedText("C")),
        correctIndex = correct,
        explanation = LocalizedText(""),
    )

    private val quiz = Quiz("quiz", "floods", LocalizedText("Quiz"), listOf(question(1), question(0), question(2)))

    @Test
    fun checkNeedsASelection() {
        val s = QuizSession(quiz).check()
        assertFalse(s.isChecked)
        assertTrue(s.answers.isEmpty())
    }

    @Test
    fun countsRightAndWrongAnswers() {
        val s = QuizSession(quiz)
            .select(1).check().next()   // right
            .select(2).check()          // wrong
        assertEquals(1, s.correctCount)
        assertEquals(1, s.wrongCount)
        assertFalse(s.isCurrentCorrect)
    }

    @Test
    fun cannotChangeAnswerAfterChecking() {
        val s = QuizSession(quiz).select(0).check().select(1)
        assertEquals(0, s.selectedIndex)
    }

    @Test
    fun nextNeedsACheckedAnswer() {
        val s = QuizSession(quiz).select(1).next()
        assertEquals(0, s.currentIndex)
    }

    @Test
    fun nextClearsSelectionAndFinishesAfterLastQuestion() {
        var s = QuizSession(quiz).select(1).check().next()
        assertEquals(1, s.currentIndex)
        assertNull(s.selectedIndex)
        assertFalse(s.isChecked)

        s = s.select(0).check().next().select(2).check().next()
        assertTrue(s.isFinished)
        assertEquals(3, s.correctCount)
        assertEquals(listOf(1, 0, 2), s.answers)
    }
}
