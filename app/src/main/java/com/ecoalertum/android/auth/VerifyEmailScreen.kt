package com.ecoalertum.android.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalertum.android.R
import kotlinx.coroutines.delay

/**
 * FR6.1: a student gets in only after clicking the link sent to their UM inbox.
 * That proves they own the address, which the Firestore rules also require.
 */
@Composable
fun VerifyEmailScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    // Seconds left before "Resend" works again. Stops students hitting Firebase's rate limit.
    var cooldown by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { viewModel.clearStatus() }
    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1_000)
            cooldown -= 1
        }
    }

    AuthScaffold(
        title = stringResource(R.string.auth_verify_title),
        subtitle = stringResource(R.string.auth_verify_subtitle, viewModel.pendingEmail()),
        modifier = modifier,
    ) {
        if (state.stillUnverified) {
            Text(
                text = stringResource(R.string.auth_verify_still_unverified),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.verificationResent) {
            Text(
                text = stringResource(R.string.auth_verify_resent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AuthErrorText(state.error)
        AuthButton(
            text = stringResource(R.string.auth_verify_done_button),
            isBusy = state.isBusy,
            onClick = { viewModel.checkVerified() },
        )
        OutlinedButton(
            onClick = {
                viewModel.resendVerification()
                cooldown = RESEND_COOLDOWN_SECONDS
            },
            enabled = !state.isBusy && cooldown == 0,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (cooldown > 0) stringResource(R.string.auth_verify_resend_wait, cooldown)
                else stringResource(R.string.auth_verify_resend),
            )
        }
        TextButton(onClick = { viewModel.signOut() }) {
            Text(stringResource(R.string.auth_verify_other_account))
        }
    }
}

private const val RESEND_COOLDOWN_SECONDS = 60
