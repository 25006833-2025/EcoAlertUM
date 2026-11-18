package com.ecoalertum.android.learn.lesson

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LessonContentParserTest {

    @Test
    fun parsesCardsInBothLanguages() {
        val json = """
            [{"id":"l1","topic":"floods","minutes":3,"title":{"en":"Floods","ms":"Banjir"},
              "cards":[{"title":{"en":"Card","ms":"Kad"},"points":[{"en":"a","ms":"a"}],"tip":{"en":"t","ms":"t"}}]}]
        """.trimIndent()
        val lesson = LessonContentParser.parse(json).single()
        assertEquals("Banjir", lesson.title.get("ms"))
        assertEquals(1, lesson.cards.size)
        assertEquals(3, lesson.minutes)
    }

    @Test
    fun skipsLessonsWithNoUsableCards() {
        val json = """[{"id":"bad","cards":[{"title":{"en":"x"},"points":[]}]}]"""
        assertTrue(LessonContentParser.parse(json).isEmpty())
    }

    /** Guards the real bundled file: one lesson per quiz topic, each card fully translated. */
    @Test
    fun bundledLessonsAreValid() {
        fun asset(path: String) = listOf("src/main/assets/$path", "app/src/main/assets/$path").map(::File).first { it.exists() }
        val lessons = LessonContentParser.parse(asset("lessons/lessons.json").readText())
        val quizTopics = Regex("\"topic\"\\s*:\\s*\"(\\w+)\"").findAll(asset("quizzes/quizzes.json").readText())
            .map { it.groupValues[1] }.toSet()

        assertEquals(quizTopics, lessons.map { it.topic }.toSet())
        lessons.forEach { lesson ->
            assertTrue("${lesson.id} needs cards", lesson.cards.size >= 3)
            lesson.cards.forEach { card ->
                assertTrue("${lesson.id} needs BM card title", card.title.ms.isNotBlank())
                assertTrue("${lesson.id} needs BM points", card.points.all { it.ms.isNotBlank() })
                assertTrue("${lesson.id} needs BM tip", card.tip.ms.isNotBlank())
            }
        }
    }
}
