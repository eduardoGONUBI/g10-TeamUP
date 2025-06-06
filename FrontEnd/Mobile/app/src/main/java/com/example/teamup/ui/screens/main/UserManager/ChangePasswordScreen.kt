// ─── ChangePasswordScreen.kt ──────────────────────────────────────────────
package com.example.teamup.ui.screens.main.UserManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
 * A simple “Change Password” screen.  The user must enter:
 *  - Current password
 *  - New password
 *  - Confirm new password
 *
 * @param changePasswordViewModel  The ViewModel to call changePassword(...)
 * @param token                    The JWT token (without “Bearer “).
 *                                 We'll add “Bearer ” inside the ViewModel.
 * @param onBack                   Called when top‐bar arrow is pressed
 * @param onPasswordChanged        Called after a successful password change; typically popBackStack
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    changePasswordViewModel: ChangePasswordViewModel,
    token: String,
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit
) {
    // 1) Local form fields
    var currentPassword by remember { mutableStateOf("") }
    var newPassword     by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible  by remember { mutableStateOf(false) }

    // 2) Observe network/validation state
    val state by changePasswordViewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        changePasswordViewModel.resetState()
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
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
            /* ─── CURRENT PASSWORD ───────────────────────────────────── */
            OutlinedTextField(
                value = currentPassword,
                onValueChange = { currentPassword = it },
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

            Spacer(modifier = Modifier.height(12.dp))

            /* ─── NEW PASSWORD ───────────────────────────────────────── */
            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                singleLine = true,
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmVisible) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            /* ─── CONFIRM NEW PASSWORD ────────────────────────────────── */
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                singleLine = true,
                visualTransformation = if (confirmVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmVisible = !confirmVisible }) {
                        Icon(
                            imageVector = if (confirmVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (confirmVisible) "Hide password" else "Show password"
                        )
                    }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            /* ─── SUBMIT BUTTON ───────────────────────────────────────── */
            Button(
                onClick = {
                    changePasswordViewModel.changePassword(
                        token,
                        currentPassword.trim(),
                        newPassword,
                        confirmPassword
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state != ChangePasswordState.Loading
            ) {
                if (state == ChangePasswordState.Loading) {
                    Text("Changing…")
                } else {
                    Text("Change Password")
                }
            }
        }

        /* ─── SHOW A SNACKBAR WHEN STATE UPDATES ───────────────────── */
        LaunchedEffect(state) {
            when (state) {
                is ChangePasswordState.Success -> {
                    val msg = (state as ChangePasswordState.Success).message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = msg,
                            duration = SnackbarDuration.Short
                        )
                    }
                    // After a short delay, reset state and pop back
                    changePasswordViewModel.resetState()
                    onPasswordChanged()
                }
                is ChangePasswordState.Error -> {
                    val errMsg = (state as ChangePasswordState.Error).message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = errMsg,
                            duration = SnackbarDuration.Short
                        )
                    }
                    changePasswordViewModel.resetState()
                }
                else -> {
                    // Idle or Loading → do nothing
                }
            }
        }
    }
}
