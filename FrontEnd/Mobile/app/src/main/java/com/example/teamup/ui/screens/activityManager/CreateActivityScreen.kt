// app/src/main/java/com/example/teamup/ui/screens/activityManager/CreateActivityScreen.kt
package com.example.teamup.ui.screens.activityManager

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    token: String,
    onCreated: (Int) -> Unit
) {
    // 1) Build repository
    val repo: ActivityRepository = remember {
        ActivityRepositoryImpl(ActivityApi.create())
    }

    // 2) Create ViewModel internally
    val viewModel: CreateActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateActivityViewModel(repo) as T
            }
        }
    )

    // 3) Observe form, sports list, and UI state
    val form by viewModel.form.collectAsState()
    val sports by viewModel.sports.collectAsState()
    val uiState by viewModel.state.collectAsState()

    val context = LocalContext.current

    // ─── Trigger initial load of sports when screen first appears ─────────────────
    LaunchedEffect(token) {
        viewModel.loadSports(token)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (uiState) {
            is CreateUiState.Loading -> {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(16.dp))
            }
            is CreateUiState.Error -> {
                Text(
                    text = (uiState as CreateUiState.Error).msg,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            is CreateUiState.Success -> {
                val newIdString = (uiState as CreateUiState.Success).createdId
                LaunchedEffect(newIdString) {
                    val newIdInt = newIdString.toIntOrNull() ?: return@LaunchedEffect
                    onCreated(newIdInt)
                    viewModel.clearStatus()
                }
            }
            else -> { /* Idle → show form */ }
        }

        OutlinedTextField(
            value = form.name,
            onValueChange = { newName ->
                viewModel.update { prev -> prev.copy(name = newName) }
            },
            label = { Text("Activity Name") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        // ─── SPORT DROPDOWN ─────────────────────────────────────────────────────────
        var sportExpanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = sportExpanded,
            onExpandedChange = { sportExpanded = !sportExpanded },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            OutlinedTextField(
                readOnly = true,
                value = form.sport?.name ?: "",
                onValueChange = { /* no-op */ },
                label = { Text("Sport") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded)
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = sportExpanded,
                onDismissRequest = { sportExpanded = false }
            ) {
                // If sports is still empty, we can show a disabled “Loading…” item
                if (sports.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("Loading sports...") },
                        onClick = { /* no-op */ },
                        enabled = false
                    )
                } else {
                    sports.forEach { sp ->
                        DropdownMenuItem(
                            text = { Text(sp.name) },
                            onClick = {
                                viewModel.update { prev -> prev.copy(sport = sp) }
                                sportExpanded = false
                            }
                        )
                    }
                }
            }
        }

        OutlinedTextField(
            value = form.place,
            onValueChange = { newPlace ->
                viewModel.update { prev -> prev.copy(place = newPlace) }
            },
            label = { Text("Location") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        )

        OutlinedTextField(
            value = form.date,
            onValueChange = { },
            label = { Text("Date (YYYY-MM-DD)") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    val now = java.util.Calendar.getInstance()
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            val dateString = "%04d-%02d-%02d".format(year, month + 1, dayOfMonth)
                            viewModel.update { prev -> prev.copy(date = dateString) }
                        },
                        now.get(java.util.Calendar.YEAR),
                        now.get(java.util.Calendar.MONTH),
                        now.get(java.util.Calendar.DAY_OF_MONTH)
                    ).show()
                }) {
                    Icon(Icons.Default.DateRange, contentDescription = "Pick date")
                }
            }
        )

        OutlinedTextField(
            value = form.time,
            onValueChange = { },
            label = { Text("Time (HH:MM)") },
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            trailingIcon = {
                IconButton(onClick = {
                    val now = java.util.Calendar.getInstance()
                    TimePickerDialog(
                        context,
                        { _, hourOfDay, minute ->
                            val timeString = "%02d:%02d".format(hourOfDay, minute)
                            viewModel.update { prev -> prev.copy(time = timeString) }
                        },
                        now.get(java.util.Calendar.HOUR_OF_DAY),
                        now.get(java.util.Calendar.MINUTE),
                        true
                    ).show()
                }) {
                    Icon(Icons.Default.AccessTime, contentDescription = "Pick time")
                }
            }
        )

        OutlinedTextField(
            value = form.max,
            onValueChange = { newMax ->
                viewModel.update { prev -> prev.copy(max = newMax) }
            },
            label = { Text("Number of Participants") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = { viewModel.submit(token) },
            enabled = uiState !is CreateUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Activity")
        }
    }
}
