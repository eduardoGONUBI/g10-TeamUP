// RegisterScreen.kt
package com.example.teamup.ui.screens.main.UserManager

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import kotlinx.coroutines.launch

/**
 * A Composable screen that allows a new user to register.
 *
 * @param registerViewModel   The ViewModel that holds formState & registerState
 * @param onBackToLogin       Called when the top-bar back arrow is pressed
 * @param onRegistrationDone  Called after successful registration (e.g. navigate back to Login)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onBackToLogin: () -> Unit,
    onRegistrationDone: () -> Unit
) {
    // 1) Observe the form-values from the ViewModel
    val formState by registerViewModel.formState.collectAsState()
    // 2) Observe the current registration/network state
    val registerState by registerViewModel.registerState.collectAsState()

    // 3) A SnackbarHostState to show error or success messages
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope   = rememberCoroutineScope()
    val context          = LocalContext.current

    // Ensure Places SDK is initialized (do this once, e.g. in your Application.onCreate())
    if (!Places.isInitialized()) {
        Places.initialize(context.applicationContext, "<YOUR_GOOGLE_API_KEY>")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Reset ViewModel state before going back
                        registerViewModel.resetState()
                        onBackToLogin()
                    }) {
                        Icon(
                            imageVector   = Icons.Default.ArrowBack,
                            contentDescription = "Back to login"
                        )
                    }
                }
            )
        },
        // 4) Hook up the Snackbar host
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement   = Arrangement.Top,
            horizontalAlignment   = Alignment.CenterHorizontally
        ) {
            // ────────────────────────────────────────────────────
            // Username Field
            OutlinedTextField(
                value            = formState.name,
                onValueChange    = { new -> registerViewModel.updateForm { it.copy(name = new) } },
                label            = { Text("Username") },
                singleLine       = true,
                modifier         = Modifier.fillMaxWidth(),
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ────────────────────────────────────────────────────
            // Email Field
            OutlinedTextField(
                value            = formState.email,
                onValueChange    = { new -> registerViewModel.updateForm { it.copy(email = new) } },
                label            = { Text("Email") },
                singleLine       = true,
                keyboardOptions  = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier         = Modifier.fillMaxWidth(),
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ────────────────────────────────────────────────────
            // Password Field
            var passwordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value            = formState.password,
                onValueChange    = { new -> registerViewModel.updateForm { it.copy(password = new) } },
                label            = { Text("Password") },
                singleLine       = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon     = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector   = if (passwordVisible)
                                Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible)
                                "Hide password"
                            else "Show password"
                        )
                    }
                },
                modifier         = Modifier.fillMaxWidth(),
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ────────────────────────────────────────────────────
            // Confirm Password Field
            var confirmPasswordVisible by remember { mutableStateOf(false) }
            OutlinedTextField(
                value            = formState.confirmPassword,
                onValueChange    = { new -> registerViewModel.updateForm { it.copy(confirmPassword = new) } },
                label            = { Text("Confirm Password") },
                singleLine       = true,
                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None
                else PasswordVisualTransformation(),
                trailingIcon     = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            imageVector   = if (confirmPasswordVisible)
                                Icons.Default.VisibilityOff
                            else Icons.Default.Visibility,
                            contentDescription = if (confirmPasswordVisible)
                                "Hide password"
                            else "Show password"
                        )
                    }
                },
                modifier         = Modifier.fillMaxWidth(),
                colors           = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor   = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ────────────────────────────────────────────────────
            // City / Location Field: launch Google Places Autocomplete
            CityAutocompleteField(
                currentCity  = formState.location,
                onCitySelected = { selectedCity ->
                    registerViewModel.updateForm { it.copy(location = selectedCity) }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ────────────────────────────────────────────────────
            // Register Button
            Button(
                onClick         = { registerViewModel.register() },
                modifier        = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                enabled         = registerState != RegisterState.Loading
            ) {
                if (registerState == RegisterState.Loading) {
                    Text("Registering…")
                } else {
                    Text("Register")
                }
            }
        }

        // ────────────────────────────────────────────────────
        // Show a Snackbar on Error or Success
        LaunchedEffect(registerState) {
            when (registerState) {
                is RegisterState.Error -> {
                    val msg = (registerState as RegisterState.Error).message
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message  = msg,
                            duration = SnackbarDuration.Short
                        )
                    }
                    // Reset back to Idle so user can correct input
                    registerViewModel.resetState()
                }

                is RegisterState.Success -> {
                    // First show “Check your email” Snackbar, wait until dismissed...
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            message  = (registerState as RegisterState.Success).message,
                            duration = SnackbarDuration.Short
                        )
                        // AFTER snackbar is dismissed, navigate back to Login:
                        registerViewModel.resetState()
                        onRegistrationDone()
                    }
                }

                else -> {
                    // Idle or Loading → no Snackbar
                }
            }
        }
    }
}

/**
 * A lightweight wrapper that uses Google Places Autocomplete in "FULLSCREEN" mode
 * to force the user to pick a city/village name.  You must have added:
 *
 *   implementation "com.google.android.libraries.places:places:<latest-version>"
 *
 * and called `Places.initialize(...)` in your Application or before this screen is first shown.
 */
@Composable
fun CityAutocompleteField(
    currentCity: String,
    onCitySelected: (String) -> Unit
) {
    val context = LocalContext.current

    // Register for Activity Result to receive a Place object
    val autocompleteLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val place = Autocomplete.getPlaceFromIntent(data!!)
                // We only asked for Place.Field.NAME, so that's guaranteed non‐null
                onCitySelected(place.name!!)
            }
        }

    OutlinedTextField(
        value            = currentCity,
        onValueChange    = { /* read‐only; must pick from Autocomplete */ },
        enabled          = false,
        label            = { Text("City") },
        singleLine       = true,
        modifier         = Modifier
            .fillMaxWidth()
            .clickable {
                // When clicked, launch Google’s Autocomplete overlay limited to cities
                val fields = listOf(Place.Field.ID, Place.Field.NAME)
                val intent = Autocomplete.IntentBuilder(
                    AutocompleteActivityMode.FULLSCREEN,
                    fields
                )
                    .setTypesFilter(listOf("locality", "sublocality", "postal_town"))
                    .build(context)
                autocompleteLauncher.launch(intent)
            },
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor   = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}
