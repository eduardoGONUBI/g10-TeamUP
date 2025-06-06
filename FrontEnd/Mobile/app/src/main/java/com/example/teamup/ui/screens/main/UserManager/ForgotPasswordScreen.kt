package com.example.teamup.ui.screens.main.UserManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * A simple “Forgot Password” screen where the user enters their email and taps “Send reset link.”
 *
 * @param forgotPasswordViewModel  The ViewModel that handles the network call
 * @param onBack                   Called when the top app bar back arrow is pressed
 * @param onResetLinkSent          Called after a successful “send reset link” operation
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    forgotPasswordViewModel: ForgotPasswordViewModel,
    onBack: () -> Unit,
    onResetLinkSent: () -> Unit
) {
    // 1) Observe the email form field and the network state from the ViewModel
    var email by remember { mutableStateOf("") }
    val state by forgotPasswordViewModel.state.collectAsState()

    // 2) Snackbar host to show success/error messages
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Forgot Password") },
                navigationIcon = {
                    IconButton(onClick = {
                        // reset any error/success in the VM, then navigate back
                        forgotPasswordViewModel.resetState()
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
            Text(
                text = "Enter your email address below. We’ll send you a password reset link.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    forgotPasswordViewModel.requestReset(email.trim())
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled = state !is ForgotPasswordState.Loading
            ) {
                when (state) {
                    is ForgotPasswordState.Loading -> Text("Sending…")
                    else                           -> Text("Send reset link")
                }
            }
        }

        // 3) Show a Snackbar on Success or Error
        LaunchedEffect(state) {
            when (state) {
                is ForgotPasswordState.Success -> {
                    val message = (state as ForgotPasswordState.Success).message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = SnackbarDuration.Short
                        )
                    }
                    // After showing success, reset state and call onResetLinkSent()
                    forgotPasswordViewModel.resetState()
                    onResetLinkSent()
                }
                is ForgotPasswordState.Error -> {
                    val errorMsg = (state as ForgotPasswordState.Error).message
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = errorMsg,
                            duration = SnackbarDuration.Short
                        )
                    }
                    // Reset to idle so user can try again
                    forgotPasswordViewModel.resetState()
                }
                else -> {
                    // Idle or Loading: do nothing
                }
            }
        }
    }
}
