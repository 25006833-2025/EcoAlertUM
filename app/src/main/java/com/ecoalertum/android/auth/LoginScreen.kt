package com.ecoalertum.android.auth

import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ecoalertum.android.R

/** FR6.1: log in with a UM student email. */
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onRegister: () -> Unit,
    onForgotPassword: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var email by rememberSaveable { mutableStateOf("") }
    // Not saved across rotation on purpose: passwords should not sit in saved instance state.
    var password by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearStatus() }

    AuthScaffold(
        title = stringResource(R.string.auth_login_title),
        subtitle = stringResource(R.string.auth_login_subtitle),
        modifier = modifier,
    ) {
        EmailField(email, { email = it }, isError = state.error.isEmailError())
        PasswordField(
            value = password,
            onValueChange = { password = it },
            labelRes = R.string.auth_password_label,
            isError = state.error == AuthError.WRONG_CREDENTIALS || state.error == AuthError.PASSWORD_REQUIRED,
            imeAction = ImeAction.Done,
            onDone = { viewModel.signIn(email, password) },
        )
        AuthErrorText(state.error)
        AuthButton(
            text = stringResource(R.string.auth_login_button),
            isBusy = state.isBusy,
            onClick = { viewModel.signIn(email, password) },
        )
        TextButton(onClick = onForgotPassword) { Text(stringResource(R.string.auth_forgot_password)) }
        TextButton(onClick = onRegister) { Text(stringResource(R.string.auth_go_to_register)) }
    }
}
