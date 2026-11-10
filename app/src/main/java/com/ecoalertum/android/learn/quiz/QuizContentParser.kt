package com.ecoalertum.android.learn.quiz

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
 * Reads `assets/quizzes/quizzes.json`. Questions with no options or an out-of-range answer
 * are skipped, and so are quizzes left with no questions, so a typo never crashes the app.
 */
object QuizContentParser {

    fun parse(json: String): List<Quiz> =
        Json.parseToJsonElement(json).jsonArray.mapNotNull { it.jsonObject.toQuiz() }

    private fun JsonObject.toQuiz(): Quiz? {
        val id = string("id") ?: return null
        val questions = this["questions"]?.jsonArray?.mapNotNull { it.jsonObject.toQuestion() }.orEmpty()
        if (questions.isEmpty()) return null
        return Quiz(
            id = id,
            topic = string("topic").orEmpty(),
            title = text("title"),
            questions = questions,
        )
    }

    private fun JsonObject.toQuestion(): QuizQuestion? {
        val options = this["options"]?.jsonArray?.map { it.toText() }.orEmpty()
        val answer = this["answer"]?.jsonPrimitive?.intOrNull ?: return null
        if (options.size < 2 || answer !in options.indices) return null
        return QuizQuestion(
            id = string("id").orEmpty(),
            prompt = text("prompt"),
            options = options,
            correctIndex = answer,
            explanation = text("explanation"),
        )
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
