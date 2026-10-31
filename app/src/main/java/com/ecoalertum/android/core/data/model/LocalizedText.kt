package com.ecoalertum.android.core.data.model

/** System-written text in both app languages. */
data class LocalizedText(
    val en: String = "",
    val ms: String = "",
) {
    /** Text for [languageCode] ("en" or "ms"), falling back to English. */
    fun get(languageCode: String): String = if (languageCode == "ms" && ms.isNotBlank()) ms else en
}
