// ─── ChangeEmailScreen.kt ────────────────────────────────────────────────
package com.example.teamup.ui.screens.main.UserManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A Composable screen that lets the user enter:
 *   • New email
 *   • Current password
 *
 * Then calls `changeEmailViewModel.changeEmail(...)`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeEmailScreen(
    changeEmailViewModel: ChangeEmailViewModel,
    token: String,
    onBack: () -> Unit,
    onEmailChanged: () -> Unit
) {
    // Local form fields
    var newEmail        by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // Observe ViewModel state
    val state by changeEmailViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Email") },
                navigationIcon = {
                    IconButton(onClick = {
                        changeEmailViewModel.resetState()
                        onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            /* ─── NEW EMAIL FIELD ────────────────────────────────────────────── */
            OutlinedTextField(
                value = newEmail,
                onValueChange = { newEmail = it },
                label = { Text("New Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            /* ─── CURRENT PASSWORD FIELD ─────────────────────────────────────── */
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Current Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            /* ─── CHANGE EMAIL BUTTON ────────────────────────────────────────── */
            Button(
                onClick = {
                    changeEmailViewModel.changeEmail(
                        token,
                        newEmail.trim(),
                        password
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state != ChangeEmailState.Loading
            ) {
                if (state == ChangeEmailState.Loading) {
                    Text("Changing…")
                } else {
                    Text("Change Email")
                }
            }
        }

        /* ─── SHOW SNACKBAR ON ERROR / SUCCESS ───────────────────────────── */
        LaunchedEffect(state) {
            when (state) {
                is ChangeEmailState.Success -> {
                    val msg = (state as ChangeEmailState.Success).message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                    }
                    // Reset and go back to EditProfile
                    changeEmailViewModel.resetState()
                    onEmailChanged()
                }
                is ChangeEmailState.Error -> {
                    val errMsg = (state as ChangeEmailState.Error).message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = errMsg,
                            duration = SnackbarDuration.Short
                        )
                    }
                    changeEmailViewModel.resetState()
                }
                else -> { /* Idle or Loading → do nothing */ }
            }
        }
    }
}
