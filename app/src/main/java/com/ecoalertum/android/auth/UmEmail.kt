package com.ecoalertum.android.auth

/**
 * FR6.1: the only email addresses that may use the app.
 *
 * This check is for a clear error message. The real enforcement is in `firestore.rules`
 * (verified email ending in @siswa.um.edu.my), because an app-side check can be bypassed.
 */
object UmEmail {
    const val DOMAIN = "@siswa.um.edu.my"

    /** Trims spaces and lower-cases, so " Name@Siswa.UM.edu.my " is accepted. */
    fun normalize(raw: String): String = raw.trim().lowercase()

    /** True for `something@siswa.um.edu.my`: one `@`, a non-empty name before it, no spaces. */
    fun isValid(raw: String): Boolean {
        val email = normalize(raw)
        if (!email.endsWith(DOMAIN)) return false
        val local = email.removeSuffix(DOMAIN)
        return local.isNotEmpty() && '@' !in local && local.none { it.isWhitespace() }
    }
}
