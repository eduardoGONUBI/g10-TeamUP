// File: app/src/main/java/com/example/teamup/ui/screens/EditProfileScreen.kt
package com.example.teamup.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.R
import com.example.teamup.data.remote.Repository.UserRepositoryImpl
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.remote.model.SportDto
import com.example.teamup.presentation.profile.EditProfileUiState
import com.example.teamup.presentation.profile.EditProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    token: String,
    usernameInitial: String,
    locationInitial: String,
    sportsInitial: List<Int>,
    onFinished: (deleted: Boolean) -> Unit, // called after success or delete
    onBack: () -> Unit
) {
    // ─── ViewModel instantiation ─────────────────────────────────────────────
    val vm: EditProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return EditProfileViewModel(
                    UserRepositoryImpl(AuthApi.create())
                ) as T
            }
        }
    )

    // ─── Collect UI state from ViewModel ────────────────────────────────────
    val uiState by vm.ui.collectAsState()
    val allSports by vm.sports.collectAsState()
    val errorLoadingSports by vm.error.collectAsState()

    // ─── Local form state ───────────────────────────────────────────────────
    var username by remember { mutableStateOf(usernameInitial) }
    var location by remember { mutableStateOf(locationInitial) }

    // Dropdown state: we let the user pick exactly one sport from the list
    var chosenSportName by remember { mutableStateOf("") }
    var chosenSportId by remember { mutableStateOf<Int?>(null) }
    var expanded by remember { mutableStateOf(false) }

    // Track whether the user tapped “Delete Account”
    var didDelete by remember { mutableStateOf(false) }

    // ─── Side effects ───────────────────────────────────────────────────────
    // Load the list of sports once when this screen appears
    LaunchedEffect(token) {
        vm.loadSports("Bearer $token")

        // Pre-select the first initial sport if sportsInitial is non-empty
        sportsInitial.firstOrNull()?.let { initialId ->
            allSports.find { it.id == initialId }?.let { dto ->
                chosenSportName = dto.name
                chosenSportId = dto.id
            }
        }
    }

    // Whenever uiState.done becomes true, call onFinished(...)
    LaunchedEffect(uiState.done) {
        if (uiState.done) {
            onFinished(didDelete)
        }
    }

    // ─── Screen UI ───────────────────────────────────────────────────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopAppBar(
                title = { Text(text = "Edit Profile", fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── Avatar placeholder ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {}
                Icon(
                    painter = painterResource(id = R.drawable.change_profile_pic),
                    contentDescription = "Change photo",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(32.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape)
                        .padding(4.dp)
                        .clickable { /* TODO: open image picker */ }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // ── Form fields ────────────────────────────────────────────────────
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Favourite Sport dropdown
                Text(text = "Favourite Sport", style = MaterialTheme.typography.bodyMedium)
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = chosenSportName,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Choose a sport") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        allSports.forEach { sportDto: SportDto ->
                            DropdownMenuItem(
                                text = { Text(sportDto.name) },
                                onClick = {
                                    chosenSportName = sportDto.name
                                    chosenSportId = sportDto.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Location
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Location") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Save button
                Button(
                    enabled = !uiState.saving,
                    onClick = {
                        val sportIds = chosenSportId?.let { listOf(it) } ?: emptyList()
                        vm.save("Bearer $token", username, location, sportIds)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (uiState.saving) "Saving…" else "Save")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Change Password button (placeholder)
                OutlinedButton(
                    onClick = { /* TODO: navigate to change password */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Change Password")
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Change Email button (placeholder)
                OutlinedButton(
                    onClick = { /* TODO: navigate to change email */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MailOutline, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Change Email")
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Delete Account
                OutlinedButton(
                    enabled = !uiState.saving,
                    onClick = {
                        didDelete = true
                        vm.deleteAccount("Bearer $token")
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "Delete Account")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }

        // ── Snackbar for update/delete errors ───────────────────────────────
        uiState.error?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = "Error: $msg")
            }
        }

        // ── Snackbar for “failed to load sports” ────────────────────────────
        errorLoadingSports?.let { msg ->
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text(text = "Failed to load sports: $msg")
            }
        }
    }
}
