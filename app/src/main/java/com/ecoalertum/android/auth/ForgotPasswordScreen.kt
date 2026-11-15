package com.ecoalertum.android.auth

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalertum.android.R

/** FR6.3: reset the password through the UM email. */
@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearStatus() }

    AuthScaffold(
        title = stringResource(R.string.auth_reset_title),
        subtitle = stringResource(R.string.auth_reset_subtitle),
        modifier = modifier,
    ) {
        EmailField(
            value = email,
            onValueChange = { email = it },
            isError = state.error.isEmailError(),
            imeAction = ImeAction.Done,
            onDone = { viewModel.sendPasswordReset(email) },
        )
        AuthErrorText(state.error)
        if (state.resetSent) {
            Text(
                text = stringResource(R.string.auth_reset_sent),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        AuthButton(
            text = stringResource(R.string.auth_reset_button),
            isBusy = state.isBusy,
            onClick = { viewModel.sendPasswordReset(email) },
        )
        TextButton(onClick = onBackToLogin) { Text(stringResource(R.string.auth_back_to_login)) }
    }
}
