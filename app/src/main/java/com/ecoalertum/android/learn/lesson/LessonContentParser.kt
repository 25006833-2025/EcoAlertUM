package com.ecoalertum.android.learn.lesson

import com.ecoalertum.android.core.data.model.LocalizedText
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Reads `assets/lessons/lessons.json`. Cards with no points are skipped, and so are lessons
 * left with no cards, so a typo in the file never crashes the app.
 */
object LessonContentParser {

    fun parse(json: String): List<Lesson> =
        Json.parseToJsonElement(json).jsonArray.mapNotNull { it.jsonObject.toLesson() }

    private fun JsonObject.toLesson(): Lesson? {
        val id = string("id") ?: return null
        val cards = this["cards"]?.jsonArray?.mapNotNull { it.jsonObject.toCard() }.orEmpty()
        if (cards.isEmpty()) return null
        return Lesson(
            id = id,
            topic = string("topic").orEmpty(),
            title = text("title"),
            minutes = this["minutes"]?.jsonPrimitive?.intOrNull ?: 3,
            cards = cards,
        )
    }

    private fun JsonObject.toCard(): LessonCard? {
        val points = this["points"]?.jsonArray?.map { it.toText() }.orEmpty()
        if (points.isEmpty()) return null
        return LessonCard(title = text("title"), points = points, tip = text("tip"))
    }

    private fun JsonObject.string(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull

    private fun JsonObject.text(key: String): LocalizedText = this[key]?.toText() ?: LocalizedText()

    private fun JsonElement.toText(): LocalizedText {
        val obj = jsonObject
        return LocalizedText(
            en = obj["en"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            ms = obj["ms"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )
    }
}
