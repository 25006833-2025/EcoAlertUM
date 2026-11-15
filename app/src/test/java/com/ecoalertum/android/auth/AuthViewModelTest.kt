package com.ecoalertum.android.auth

import com.ecoalertum.android.core.navigation.Routes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Login logic without Firebase: the repository is a fake. */
@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private class FakeAuthRepository(initial: AuthUser? = null) : AuthRepository {
        val user = MutableStateFlow(initial)
        var signInResult: Result<AuthUser> = Result.failure(AuthException(AuthError.WRONG_CREDENTIALS))
        var registerResult: Result<Unit> = Result.success(Unit)
        var verifiedAfterRefresh = false
        var resetResult: Result<Unit> = Result.success(Unit)
        var profile: Result<UserProfile> = Result.success(UserProfile("Aisyah", "student"))
        var signInCalls = 0
        var registerCalls = 0
        var resetCalls = 0

        override val currentUser: AuthUser? get() = user.value
        override fun observeUser(): Flow<AuthUser?> = user

        override suspend fun register(displayName: String, email: String, password: String): Result<Unit> {
            registerCalls++
            return registerResult
        }

        override suspend fun signIn(email: String, password: String): Result<AuthUser> {
            signInCalls++
            signInResult.onSuccess { user.value = it }
            return signInResult
        }

        override suspend fun resendVerification(): Result<Unit> = Result.success(Unit)
        override suspend fun refreshVerification(): Result<Boolean> = Result.success(verifiedAfterRefresh)

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            resetCalls++
            return resetResult
        }

        override suspend fun loadProfile(): Result<UserProfile> = profile
        override fun signOut() {
            user.value = null
        }
    }

    private val verified = AuthUser("u1", "aisyah@siswa.um.edu.my", "Aisyah", isEmailVerified = true)
    private val unverified = verified.copy(isEmailVerified = false)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startRouteDependsOnSavedLogin() {
        assertEquals(Routes.LOGIN, AuthViewModel(FakeAuthRepository(null)).startRoute)
        assertEquals(Routes.VERIFY_EMAIL, AuthViewModel(FakeAuthRepository(unverified)).startRoute)
        assertEquals(Routes.MAIN_GRAPH, AuthViewModel(FakeAuthRepository(verified)).startRoute)
    }

    @Test
    fun nonUmEmailIsRejectedWithoutCallingFirebase() {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)
        vm.signIn("someone@gmail.com", "password123")
        assertEquals(AuthError.INVALID_EMAIL_DOMAIN, vm.uiState.value.error)
        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun emptyPasswordIsRejected() {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)
        vm.signIn("aisyah@siswa.um.edu.my", "")
        assertEquals(AuthError.PASSWORD_REQUIRED, vm.uiState.value.error)
        assertEquals(0, repo.signInCalls)
    }

    @Test
    fun wrongPasswordShowsError() {
        val vm = AuthViewModel(FakeAuthRepository())
        vm.signIn("aisyah@siswa.um.edu.my", "wrong-password")
        assertEquals(AuthError.WRONG_CREDENTIALS, vm.uiState.value.error)
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test
    fun verifiedLoginOpensTheApp() = runTest {
        val repo = FakeAuthRepository().apply { signInResult = Result.success(verified) }
        val vm = AuthViewModel(repo)
        vm.signIn("Aisyah@siswa.um.edu.my", "password123")
        assertEquals(AuthEvent.LoggedIn, vm.authEvents.first())
        assertEquals("Aisyah", vm.session.value?.displayName)
        assertFalse(vm.session.value!!.isModerator)
    }

    @Test
    fun moderatorRoleIsPickedUpAtLogin() = runTest {
        val repo = FakeAuthRepository().apply {
            signInResult = Result.success(verified)
            profile = Result.success(UserProfile("Hafiz", "moderator"))
        }
        val vm = AuthViewModel(repo)
        vm.signIn("aisyah@siswa.um.edu.my", "password123")
        assertEquals(AuthEvent.LoggedIn, vm.authEvents.first())
        assertTrue(vm.session.value!!.isModerator)
    }

    @Test
    fun unverifiedLoginGoesToVerifyScreenAndHasNoSession() = runTest {
        val repo = FakeAuthRepository().apply { signInResult = Result.success(unverified) }
        val vm = AuthViewModel(repo)
        vm.signIn("aisyah@siswa.um.edu.my", "password123")
        assertEquals(AuthEvent.NeedsVerification, vm.authEvents.first())
        assertNull(vm.session.value)
    }

    @Test
    fun registerChecksPasswordsAndName() {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)
        vm.register("", "aisyah@siswa.um.edu.my", "password123", "password123")
        assertEquals(AuthError.NAME_REQUIRED, vm.uiState.value.error)
        vm.register("Aisyah", "aisyah@gmail.com", "password123", "password123")
        assertEquals(AuthError.INVALID_EMAIL_DOMAIN, vm.uiState.value.error)
        vm.register("Aisyah", "aisyah@siswa.um.edu.my", "short", "short")
        assertEquals(AuthError.WEAK_PASSWORD, vm.uiState.value.error)
        vm.register("Aisyah", "aisyah@siswa.um.edu.my", "password123", "password124")
        assertEquals(AuthError.PASSWORDS_DONT_MATCH, vm.uiState.value.error)
        assertEquals(0, repo.registerCalls)
    }

    @Test
    fun successfulRegisterGoesToVerifyScreen() = runTest {
        val vm = AuthViewModel(FakeAuthRepository())
        vm.register("Aisyah", "aisyah@siswa.um.edu.my", "password123", "password123")
        assertEquals(AuthEvent.NeedsVerification, vm.authEvents.first())
    }

    @Test
    fun emailInUseShowsError() {
        val repo = FakeAuthRepository().apply {
            registerResult = Result.failure(AuthException(AuthError.EMAIL_IN_USE))
        }
        val vm = AuthViewModel(repo)
        vm.register("Aisyah", "aisyah@siswa.um.edu.my", "password123", "password123")
        assertEquals(AuthError.EMAIL_IN_USE, vm.uiState.value.error)
    }

    @Test
    fun checkVerifiedStaysOnScreenUntilTheLinkIsOpened() = runTest {
        val repo = FakeAuthRepository(unverified)
        val vm = AuthViewModel(repo)
        vm.checkVerified()
        assertTrue(vm.uiState.value.stillUnverified)

        repo.verifiedAfterRefresh = true
        repo.user.value = verified
        vm.checkVerified()
        assertEquals(AuthEvent.LoggedIn, vm.authEvents.first())
    }

    @Test
    fun resetRejectsNonUmEmail() {
        val repo = FakeAuthRepository()
        val vm = AuthViewModel(repo)
        vm.sendPasswordReset("someone@gmail.com")
        assertEquals(AuthError.INVALID_EMAIL_DOMAIN, vm.uiState.value.error)
        assertEquals(0, repo.resetCalls)
    }

    @Test
    fun resetReportsSuccessEvenForUnknownEmail() {
        val repo = FakeAuthRepository().apply {
            resetResult = Result.failure(AuthException(AuthError.WRONG_CREDENTIALS))
        }
        val vm = AuthViewModel(repo)
        vm.sendPasswordReset("nobody@siswa.um.edu.my")
        assertTrue(vm.uiState.value.resetSent)
        assertNull(vm.uiState.value.error)
    }

    @Test
    fun signOutClearsSessionAndSendsToLogin() = runTest {
        val repo = FakeAuthRepository(verified)
        val vm = AuthViewModel(repo)
        vm.signOut()
        assertNull(vm.session.value)
        assertEquals(AuthEvent.LoggedOut, vm.authEvents.first())
    }
}
