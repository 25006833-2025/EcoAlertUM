package com.ecoalertum.android.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ecoalertum.android.EcoAlertApplication
import com.ecoalertum.android.core.navigation.Routes
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** What the login, sign-up, verify and reset screens show. */
data class AuthUiState(
    val isBusy: Boolean = false,
    val error: AuthError? = null,
    /** FR6.3: the reset email was requested. */
    val resetSent: Boolean = false,
    val verificationResent: Boolean = false,
    /** The student tapped "I've verified" but Firebase still says the email is not verified. */
    val stillUnverified: Boolean = false,
)

/** The signed-in student, for the side menu and the moderator check. */
data class Session(val email: String, val displayName: String, val isModerator: Boolean)

/** One-off navigation requests. EcoAlertApp turns them into navigation calls. */
sealed interface AuthEvent {
    data object LoggedIn : AuthEvent
    data object NeedsVerification : AuthEvent
    data object LoggedOut : AuthEvent
}

/**
 * Login logic for Module 6. One instance is shared by the whole app (created in EcoAlertApp),
 * so the side menu and the login screens see the same session.
 */
class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val startUser = repository.currentUser

    // Until the profile loads, a verified user is shown as a student.
    private val _session = MutableStateFlow(
        startUser?.takeIf { it.isEmailVerified }?.let { Session(it.email, it.nameOrEmail(), isModerator = false) },
    )
    val session: StateFlow<Session?> = _session.asStateFlow()

    private val events = Channel<AuthEvent>(Channel.BUFFERED)
    val authEvents: Flow<AuthEvent> = events.receiveAsFlow()

    /** Where the app opens: login, the verify screen, or straight into the app. */
    val startRoute: String = when {
        startUser == null -> Routes.LOGIN
        startUser.isEmailVerified -> Routes.MAIN_GRAPH
        else -> Routes.VERIFY_EMAIL
    }

    init {
        if (_session.value != null) viewModelScope.launch { applyProfile() }
        // Account removed or disabled elsewhere: send the student back to login.
        viewModelScope.launch {
            repository.observeUser().collect { user ->
                if (user == null && _session.value != null) {
                    _session.value = null
                    events.send(AuthEvent.LoggedOut)
                }
            }
        }
    }

    /** The address a verification email was sent to. */
    fun pendingEmail(): String = repository.currentUser?.email.orEmpty()

    /** Called when a screen opens, so an old error does not show up. */
    fun clearStatus() {
        _uiState.value = AuthUiState()
    }

    fun signIn(email: String, password: String) {
        val normalized = UmEmail.normalize(email)
        val problem = when {
            !UmEmail.isValid(normalized) -> AuthError.INVALID_EMAIL_DOMAIN
            password.isEmpty() -> AuthError.PASSWORD_REQUIRED
            else -> null
        }
        if (problem != null) return fail(problem)
        runBusy {
            repository.signIn(normalized, password)
                .onSuccess { user ->
                    if (user.isEmailVerified) completeLogin() else events.send(AuthEvent.NeedsVerification)
                }
                .onFailure { fail(it) }
        }
    }

    fun register(name: String, email: String, password: String, confirmPassword: String) {
        val normalized = UmEmail.normalize(email)
        val problem = when {
            name.isBlank() -> AuthError.NAME_REQUIRED
            !UmEmail.isValid(normalized) -> AuthError.INVALID_EMAIL_DOMAIN
            password.length < MIN_PASSWORD_LENGTH -> AuthError.WEAK_PASSWORD
            password != confirmPassword -> AuthError.PASSWORDS_DONT_MATCH
            else -> null
        }
        if (problem != null) return fail(problem)
        runBusy {
            repository.register(name.trim(), normalized, password)
                .onSuccess { events.send(AuthEvent.NeedsVerification) }
                .onFailure { fail(it) }
        }
    }

    /** "I've verified my email": re-checks with Firebase and continues if it is true. */
    fun checkVerified() {
        runBusy {
            repository.refreshVerification()
                .onSuccess { verified ->
                    if (verified) completeLogin() else _uiState.update { it.copy(stillUnverified = true) }
                }
                .onFailure { fail(it) }
        }
    }

    fun resendVerification() {
        runBusy {
            repository.resendVerification()
                .onSuccess { _uiState.update { it.copy(verificationResent = true) } }
                .onFailure { fail(it) }
        }
    }

    /** FR6.3. Always reports success for unknown addresses, so the app does not reveal who has an account. */
    fun sendPasswordReset(email: String) {
        val normalized = UmEmail.normalize(email)
        if (!UmEmail.isValid(normalized)) return fail(AuthError.INVALID_EMAIL_DOMAIN)
        runBusy {
            repository.sendPasswordReset(normalized)
                .onSuccess { _uiState.update { it.copy(resetSent = true) } }
                .onFailure { e ->
                    if ((e as? AuthException)?.error == AuthError.WRONG_CREDENTIALS) {
                        _uiState.update { it.copy(resetSent = true) }
                    } else {
                        fail(e)
                    }
                }
        }
    }

    fun signOut() {
        _session.value = null
        repository.signOut()
        viewModelScope.launch { events.send(AuthEvent.LoggedOut) }
    }

    private suspend fun completeLogin() {
        applyProfile()
        events.send(AuthEvent.LoggedIn)
    }

    /** Reads `users/{uid}` for the name and role. If that fails (offline), the student still gets in. */
    private suspend fun applyProfile() {
        val user = repository.currentUser ?: return
        val profile = repository.loadProfile().getOrNull()
        _session.value = Session(
            email = user.email,
            displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: user.nameOrEmail(),
            isModerator = profile?.role == ROLE_MODERATOR || profile?.role == ROLE_ADMIN,
        )
    }

    private fun runBusy(block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = AuthUiState(isBusy = true)
            try {
                block()
            } finally {
                _uiState.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun fail(error: AuthError) {
        _uiState.update { it.copy(error = error) }
    }

    private fun fail(throwable: Throwable) {
        fail((throwable as? AuthException)?.error ?: AuthError.UNKNOWN)
    }

    private fun AuthUser.nameOrEmail(): String = displayName?.takeIf { it.isNotBlank() } ?: email.substringBefore('@')

    companion object {
        const val MIN_PASSWORD_LENGTH = 8
        private const val ROLE_MODERATOR = "moderator"
        private const val ROLE_ADMIN = "admin"

        val Factory = viewModelFactory {
            initializer {
                val container = (this[APPLICATION_KEY] as EcoAlertApplication).container
                AuthViewModel(container.authRepository)
            }
        }
    }
}
