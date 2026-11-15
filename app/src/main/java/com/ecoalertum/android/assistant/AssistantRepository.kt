package com.ecoalertum.android.assistant

import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await

class AssistantRepository(
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance(),
) {
    suspend fun explainHazard(
        alertId: String,
        language: String = "en",
    ): AssistantResponse {
        require(alertId.isNotBlank()) {
            "An alert ID is required."
        }

        val request = mapOf(
            "mode" to "explain",
            "language" to language,
            "alertId" to alertId,
        )

        val result = functions
            .getHttpsCallable("askAssistant")
            .call(request)
            .await()

        val data = result.data as? Map<*, *>
            ?: throw IllegalStateException(
                "Invalid AI Assistant response."
            )

        return AssistantResponse(
            status = data["status"] as? String
                ?: throw IllegalStateException("Missing response status."),
            explanation = data["explanation"] as? String
                ?: throw IllegalStateException("Missing explanation."),
            steps = (data["steps"] as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList(),
            sources = (data["sources"] as? List<*>)
                ?.filterIsInstance<String>()
                ?: emptyList(),
            generatedBy = data["generatedBy"] as? String
                ?: "unknown",
        )
    }
}
