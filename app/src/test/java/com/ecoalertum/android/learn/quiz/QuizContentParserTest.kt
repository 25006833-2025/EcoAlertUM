package com.ecoalertum.android.learn.quiz

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizContentParserTest {

    @Test
    fun parsesBothLanguagesAndAnswer() {
        val json = """
            [{"id":"q1","topic":"floods","title":{"en":"Floods","ms":"Banjir"},
              "questions":[{"id":"a","prompt":{"en":"Hi","ms":"Hai"},
                "options":[{"en":"x","ms":"x"},{"en":"y","ms":"y"}],"answer":1,
                "explanation":{"en":"e","ms":"e"}}]}]
        """.trimIndent()
        val quiz = QuizContentParser.parse(json).single()
        assertEquals("Banjir", quiz.title.get("ms"))
        assertEquals(1, quiz.questions.single().correctIndex)
    }

    @Test
    fun skipsBrokenQuestionsAndEmptyQuizzes() {
        val json = """
            [{"id":"bad","questions":[{"id":"a","options":[{"en":"x"},{"en":"y"}],"answer":5}]}]
        """.trimIndent()
        assertTrue(QuizContentParser.parse(json).isEmpty())
    }

    /** Guards the real bundled file: every quiz loads with 5 questions and valid answers. */
    @Test
    fun bundledQuizzesAreValid() {
        val file = listOf("src/main/assets/quizzes/quizzes.json", "app/src/main/assets/quizzes/quizzes.json")
            .map(::File).first { it.exists() }
        val quizzes = QuizContentParser.parse(file.readText())
        assertEquals(4, quizzes.size)
        quizzes.forEach { quiz ->
            assertEquals(quiz.id, 5, quiz.questions.size)
            quiz.questions.forEach { q ->
                assertTrue("${quiz.id}/${q.id} needs BM text", q.prompt.ms.isNotBlank())
                assertTrue("${quiz.id}/${q.id} needs BM options", q.options.all { it.ms.isNotBlank() })
            }
        }
    }
}
