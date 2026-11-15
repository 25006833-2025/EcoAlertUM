package com.ecoalertum.android.auth

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

/** The signed-in account, as far as the screens need to know. */
data class AuthUser(
    val uid: String,
    val email: String,
    val displayName: String?,
    val isEmailVerified: Boolean,
)

/** What the app knows about the student from `users/{uid}`. */
data class UserProfile(val displayName: String?, val role: String)

/** Reasons a login, sign-up or reset can fail. Each one has a message in `strings_auth.xml`. */
enum class AuthError {
    INVALID_EMAIL_DOMAIN,
    NAME_REQUIRED,
    PASSWORD_REQUIRED,
    WEAK_PASSWORD,
    PASSWORDS_DONT_MATCH,
    EMAIL_IN_USE,
    WRONG_CREDENTIALS,
    TOO_MANY_REQUESTS,
    NETWORK,
    UNKNOWN,
}

class AuthException(val error: AuthError, cause: Throwable? = null) : Exception(error.name, cause)

/** Login for Module 6 (FR6.1, FR6.3). Screens and the ViewModel only see this interface. */
interface AuthRepository {
    /** The signed-in user right now. Firebase restores it from the phone, so it is available at app start. */
    val currentUser: AuthUser?

    /** Emits the user on every sign-in and sign-out (null when signed out). */
    fun observeUser(): Flow<AuthUser?>

    /** Creates the account and sends the verification email. The user stays signed in but unverified. */
    suspend fun register(displayName: String, email: String, password: String): Result<Unit>

    suspend fun signIn(email: String, password: String): Result<AuthUser>

    suspend fun resendVerification(): Result<Unit>

    /** Re-reads the account from Firebase. Returns true once the student has clicked the email link. */
    suspend fun refreshVerification(): Result<Boolean>

    /** FR6.3 */
    suspend fun sendPasswordReset(email: String): Result<Unit>

    /** Refreshes the token (so the rules see `email_verified`), then reads or creates `users/{uid}`. */
    suspend fun loadProfile(): Result<UserProfile>

    fun signOut()
}

class FirebaseAuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) : AuthRepository {

    override val currentUser: AuthUser? get() = auth.currentUser?.toAuthUser()

    override fun observeUser(): Flow<AuthUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    override suspend fun register(displayName: String, email: String, password: String): Result<Unit> =
        authCall {
            val user = auth.createUserWithEmailAndPassword(email, password).await().user
                ?: error("No user returned")
            user.updateProfile(userProfileChangeRequest { this.displayName = displayName }).await()
            // The account exists now, so a failed email must not look like a failed sign-up.
            // The verify screen has a "Resend" button for that case.
            try {
                user.sendEmailVerification().await()
            } catch (e: CancellationException) {
                throw e
            } catch (ignored: Exception) {
            }
        }

    override suspend fun signIn(email: String, password: String): Result<AuthUser> = authCall {
        val user = auth.signInWithEmailAndPassword(email, password).await().user ?: error("No user returned")
        user.toAuthUser()
    }

    override suspend fun resendVerification(): Result<Unit> = authCall {
        val user = auth.currentUser ?: error("Not signed in")
        user.sendEmailVerification().await()
    }

    override suspend fun refreshVerification(): Result<Boolean> = authCall {
        val user = auth.currentUser ?: error("Not signed in")
        user.reload().await()
        // The token carries email_verified; Firestore rules read it, so it must be renewed.
        if (user.isEmailVerified) user.getIdToken(true).await()
        user.isEmailVerified
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> = authCall {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun loadProfile(): Result<UserProfile> = authCall {
        val user = auth.currentUser ?: error("Not signed in")
        val profile = withTimeoutOrNull(PROFILE_TIMEOUT_MS) {
            user.getIdToken(true).await()
            val ref = db.collection(USERS).document(user.uid)
            val snapshot = ref.get().await()
            if (snapshot.exists()) {
                UserProfile(snapshot.getString("displayName"), snapshot.getString("role") ?: ROLE_STUDENT)
            } else {
                // First login after verification. firestore.rules only lets a student create
                // their own document, and only with role = student.
                ref.set(newUserDocument(user)).await()
                UserProfile(user.displayName, ROLE_STUDENT)
            }
        }
        profile ?: error("Profile request timed out")
    }

    override fun signOut() = auth.signOut()

    // Field names follow docs/data-models.md section 4.
    private fun newUserDocument(user: FirebaseUser) = mapOf(
        "id" to user.uid,
        "email" to user.email.orEmpty(),
        "displayName" to (user.displayName ?: user.email.orEmpty().substringBefore('@')),
        "faculty" to null,
        "role" to ROLE_STUDENT,
        "language" to "en",
        "notificationsEnabled" to true,
        "fcmTokens" to emptyList<String>(),
        "badges" to emptyList<String>(),
        "createdAt" to Timestamp.now(),
    )

    private fun FirebaseUser.toAuthUser() = AuthUser(
        uid = uid,
        email = email.orEmpty(),
        displayName = displayName,
        isEmailVerified = isEmailVerified,
    )

    /** Runs [block] and turns any Firebase failure into an [AuthException] the screens can show. */
    private inline fun <T> authCall(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(AuthException(e.toAuthError(), e))
    }

    private fun Exception.toAuthError(): AuthError = when (this) {
        is FirebaseAuthUserCollisionException -> AuthError.EMAIL_IN_USE
        is FirebaseAuthWeakPasswordException -> AuthError.WEAK_PASSWORD
        // Wrong password, unknown email and malformed email all land here. The message is the
        // same on purpose, so the app does not reveal which UM emails have accounts.
        is FirebaseAuthInvalidCredentialsException,
        is FirebaseAuthInvalidUserException -> AuthError.WRONG_CREDENTIALS
        is FirebaseTooManyRequestsException -> AuthError.TOO_MANY_REQUESTS
        is FirebaseNetworkException -> AuthError.NETWORK
        else -> AuthError.UNKNOWN
    }

    private companion object {
        const val USERS = "users"
        const val ROLE_STUDENT = "student"
        const val PROFILE_TIMEOUT_MS = 8_000L
    }
}
