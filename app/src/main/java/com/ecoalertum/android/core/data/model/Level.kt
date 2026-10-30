package com.ecoalertum.android.core.data.model

/**
 * Shared four-step severity scale, from lowest to highest.
 * Declaration order is severity order, so levels compare with `<`, `>` and `maxOf`.
 *
 * [value] is the string stored in Firestore and the mock JSON. The docs call the second
 * step "alert"; the design system calls it Advisory.
 */
enum class Level(val value: String) {
    NORMAL("normal"),
    ADVISORY("alert"),
    WARNING("warning"),
    DANGER("danger");

    fun isAtLeast(other: Level): Boolean = this >= other

    companion object {
        /** Parses a stored value. Also accepts "advisory". Unknown or missing values become [NORMAL] (data-models.md). */
        fun fromValue(value: String?): Level {
            val key = value?.trim()?.lowercase()
            if (key == "advisory") return ADVISORY
            return entries.firstOrNull { it.value == key } ?: NORMAL
        }

        /** The most severe level in [levels], or [NORMAL] if there are none. */
        fun highestOf(levels: Iterable<Level>): Level = levels.maxOrNull() ?: NORMAL
    }
}
