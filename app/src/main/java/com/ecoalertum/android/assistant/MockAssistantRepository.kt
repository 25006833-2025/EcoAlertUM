package com.ecoalertum.android.assistant

import kotlinx.coroutines.delay

/**
 * Development-only AI responses.
 * Does not call Firebase or Gemini.
 */
class MockAssistantRepository {

    suspend fun explainHazard(
        alertId: String,
        language: String = "en",
    ): AssistantResponse {
        require(alertId.isNotBlank()) {
            "An alert ID is required."
        }

        // Simulate a short loading delay.
        delay(800)

        return when (alertId) {
            "Xk29fQa7LmP3vR8tY1zB" -> heavyRainResponse(language)
            "Hn4Wc8Ue2JdK6sQ0rT5v" -> riverLevelResponse(language)
            "Bq7Zp1Mx9GcV3nE5hL0a" -> airQualityResponse(language)
            else -> AssistantResponse(
                status = "ok",
                explanation = "SIMULATED RESPONSE: No mock explanation is available for this alert.",
                steps = emptyList(),
                sources = listOf("Simulated EcoAlert UM test data"),
                generatedBy = "mock",
            )
        }
    }

    private fun heavyRainResponse(language: String): AssistantResponse =
        AssistantResponse(
            status = "ok",
            explanation = if (language == "ms") {
                "SIMULASI: Amaran hujan lebat bersejarah di kampus. " +
                        "Laluan rendah mungkin berbahaya semasa hujan lebat. " +
                        "Ini bukan amaran cuaca semasa."
            } else {
                "SIMULATED: This historical alert describes heavy rainfall " +
                        "near Universiti Malaya. Low-lying paths may become unsafe. " +
                        "This is not a current weather warning."
            },
            steps = if (language == "ms") {
                listOf(
                    "Elakkan laluan rendah semasa hujan lebat.",
                    "Tunggu di dalam bangunan jika keadaan tidak selamat.",
                    "Ikuti pengumuman rasmi universiti.",
                )
            } else {
                listOf(
                    "Avoid low-lying paths during heavy rain.",
                    "Stay indoors when outdoor conditions are unsafe.",
                    "Follow official university announcements.",
                )
            },
            sources = listOf("Simulated EcoAlert UM test data"),
            generatedBy = "mock",
        )

    private fun riverLevelResponse(language: String): AssistantResponse =
        AssistantResponse(
            status = "ok",
            explanation = if (language == "ms") {
                "SIMULASI: Paras Sungai Klang meningkat dalam senario " +
                        "bersejarah ini. Ini bukan maklumat banjir semasa."
            } else {
                "SIMULATED: This historical scenario describes a rising " +
                        "water level in Sungai Klang. It does not confirm " +
                        "current flooding or river conditions."
            },
            steps = if (language == "ms") {
                listOf(
                    "Semak maklumat rasmi sebelum membuat perjalanan.",
                    "Elakkan kawasan yang dinaiki air.",
                    "Ikuti arahan keselamatan pihak berkuasa.",
                )
            } else {
                listOf(
                    "Check official updates before travelling.",
                    "Avoid flooded areas.",
                    "Follow instructions from relevant authorities.",
                )
            },
            sources = listOf("Simulated EcoAlert UM test data"),
            generatedBy = "mock",
        )

    private fun airQualityResponse(language: String): AssistantResponse =
        AssistantResponse(
            status = "ok",
            explanation = if (language == "ms") {
                "SIMULASI: Rekod kualiti udara bersejarah menunjukkan " +
                        "bacaan tidak sihat. Amaran ini telah tamat tempoh."
            } else {
                "SIMULATED: This expired historical alert describes " +
                        "unhealthy air quality. It does not represent " +
                        "current air quality conditions."
            },
            steps = if (language == "ms") {
                listOf(
                    "Semak bacaan kualiti udara terkini.",
                    "Hadkan aktiviti luar jika kualiti udara tidak sihat.",
                    "Ikuti nasihat kesihatan rasmi.",
                )
            } else {
                listOf(
                    "Check current official air quality readings.",
                    "Limit outdoor activities when air quality is unhealthy.",
                    "Follow official health guidance.",
                )
            },
            sources = listOf("Simulated EcoAlert UM test data"),
            generatedBy = "mock",
        )
}
