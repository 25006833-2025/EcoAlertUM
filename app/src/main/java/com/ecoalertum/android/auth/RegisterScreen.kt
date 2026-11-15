package com.ecoalertum.android.auth

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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

/** FR6.1: create an account. Only @siswa.um.edu.my emails are accepted, and each needs verifying. */
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var name by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { viewModel.clearStatus() }

    val submit = { viewModel.register(name, email, password, confirm) }

    AuthScaffold(
        title = stringResource(R.string.auth_register_title),
        subtitle = stringResource(R.string.auth_register_subtitle),
        modifier = modifier,
    ) {
        NameField(name, { name = it }, isError = state.error == AuthError.NAME_REQUIRED)
        EmailField(email, { email = it }, isError = state.error.isEmailError())
        PasswordField(
            value = password,
            onValueChange = { password = it },
            labelRes = R.string.auth_password_label,
            isError = state.error == AuthError.WEAK_PASSWORD,
            supportingRes = R.string.auth_password_supporting,
        )
        PasswordField(
            value = confirm,
            onValueChange = { confirm = it },
            labelRes = R.string.auth_confirm_password_label,
            isError = state.error == AuthError.PASSWORDS_DONT_MATCH,
            imeAction = ImeAction.Done,
            onDone = { submit() },
        )
        AuthErrorText(state.error)
        AuthButton(
            text = stringResource(R.string.auth_register_button),
            isBusy = state.isBusy,
            onClick = { submit() },
        )
        TextButton(onClick = onBackToLogin) { Text(stringResource(R.string.auth_back_to_login)) }
    }
}
