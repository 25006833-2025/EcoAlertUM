package com.ecoalertum.android.assistant

data class AssistantResponse(
    val status: String,
    val explanation: String,
    val steps: List<String>,
    val sources: List<String>,
    val generatedBy: String,
)
