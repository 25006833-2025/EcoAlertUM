package com.ecoalertum.android.learn.lesson

import com.ecoalertum.android.core.data.model.LocalizedText

/**
 * One short lesson (FR5.2), bundled in `assets/lessons/lessons.json`.
 * Each hazard topic has one lesson of a few swipeable cards, followed by that topic's post-quiz.
 */
data class Lesson(
    val id: String,
    /** Hazard topic: `floods`, `haze`, `storms` or `heat`. Same values as the quizzes. */
    val topic: String,
    val title: LocalizedText,
    val minutes: Int,
    val cards: List<LessonCard>,
)

data class LessonCard(
    val title: LocalizedText,
    /** Two to four short key points. */
    val points: List<LocalizedText>,
    /** "Good to know" box at the bottom of the card. */
    val tip: LocalizedText,
)
