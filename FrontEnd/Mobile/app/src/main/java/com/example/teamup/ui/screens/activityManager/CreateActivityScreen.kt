package com.example.teamup.ui.screens.activityManager

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.domain.repository.ActivityRepository
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.repository.ActivityRepositoryImpl
import com.example.teamup.domain.model.Sport
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import androidx.compose.foundation.lazy.items
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateActivityScreen(
    token: String,
    onCreated: (Int) -> Unit
) {
    val repo: ActivityRepository = remember {
        ActivityRepositoryImpl(ActivityApi.create())
    }

    val viewModel: CreateActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateActivityViewModel(repo) as T
            }
        }
    )

    // estados
    val form by viewModel.form.collectAsState()
    val sports: List<Sport> by viewModel.sports.collectAsState()
    val uiState by viewModel.state.collectAsState()

    val context = LocalContext.current

    // load sports
    LaunchedEffect(token) {
        // Make sure Places SDK is initialized
        if (!Places.isInitialized()) {
            Places.initialize(context.applicationContext, context.getString(com.example.teamup.R.string.google_maps_key))
        }
        viewModel.loadSports("Bearer $token")
    }

    // auto complete places
    val placesClient: PlacesClient = remember { Places.createClient(context) }
    var suggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    LaunchedEffect(form.place) {
        val query = form.place
        if (query.length >= 3) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(query)
                .build()
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    suggestions = response.autocompletePredictions
                }
                .addOnFailureListener {
                    suggestions = emptyList()
                }
        } else {
            suggestions = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        when (uiState) {  // mostra erro ou sucesso
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
                    onCreated(newIdInt)   // avisa a navegaçao que criou
                    viewModel.clearStatus()
                }
            }
            else -> {  }
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
                // loading sports
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

        // ─── LOCATION FIELD WITH INLINE AUTOCOMPLETE ────────────────────────────────
        OutlinedTextField(
            value = form.place,
            onValueChange = { newPlace ->
                viewModel.update { prev -> prev.copy(place = newPlace) }
            },
            label = { Text("Location") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            trailingIcon = {
                Icon(Icons.Default.Place, contentDescription = "Location icon")
            }
        )
        // Show suggestions below the text field
        if (suggestions.isNotEmpty()) {
            Card(
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .heightIn(max = 200.dp)
            ) {
                LazyColumn {
                    items(suggestions, key = { it.placeId }) { prediction ->
                        DropdownMenuItem(
                            text = { Text(prediction.getFullText(null).toString()) },
                            onClick = {
                                val chosen = prediction.getFullText(null).toString()
                                viewModel.update { prev -> prev.copy(place = chosen) }
                                suggestions = emptyList()
                            }
                        )
                    }
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

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
            onClick = { viewModel.submit("Bearer $token") },
            enabled = uiState !is CreateUiState.Loading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Create Activity")
        }
    }
}
