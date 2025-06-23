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


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    registerViewModel: RegisterViewModel,
    onBackToLogin: () -> Unit,
    onRegistrationDone: () -> Unit
) {
    // estados
    val formState by registerViewModel.formState.collectAsState()

    val registerState by registerViewModel.registerState.collectAsState()

    // snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope   = rememberCoroutineScope()
    val context          = LocalContext.current

    // api google maps places
    if (!Places.isInitialized()) {
        Places.initialize(context.applicationContext, "<YOUR_GOOGLE_API_KEY>")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Register") },
                navigationIcon = {
                    IconButton(onClick = {
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


            // City / launch Google Places Autocomplete
            CityAutocompleteField(
                currentCity  = formState.location,
                onCitySelected = { selectedCity ->
                    registerViewModel.updateForm { it.copy(location = selectedCity) }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

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

                    registerViewModel.resetState()
                }

                is RegisterState.Success -> {
                    // Check your email
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

                }
            }
        }
    }
}


// force the user to pick a city name
@Composable
fun CityAutocompleteField(
    currentCity: String,
    onCitySelected: (String) -> Unit
) {
    val context = LocalContext.current

    val autocompleteLauncher =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val place = Autocomplete.getPlaceFromIntent(data!!)
                onCitySelected(place.name!!)
            }
        }

    OutlinedTextField(
        value            = currentCity,
        onValueChange    = {  },
        enabled          = false,
        label            = { Text("City") },
        singleLine       = true,
        modifier         = Modifier
            .fillMaxWidth()
            .clickable {
                // google autocomplete
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
