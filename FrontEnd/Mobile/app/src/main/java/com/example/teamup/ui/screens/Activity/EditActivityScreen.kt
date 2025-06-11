// File: app/src/main/java/com/example/teamup/ui/screens/Activity/EditActivityScreen.kt
package com.example.teamup.ui.screens.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.widget.DatePicker
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.EventUpdateRequest
import com.example.teamup.data.remote.model.SportDto
import com.example.teamup.ui.popups.DeleteActivityDialog
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onSave: () -> Unit,    // Call this to navigate back + trigger detail refresh
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()

    // ─── Snackbar state ───────────────────────────────────────────────
    val snackbarHostState = remember { SnackbarHostState() }

    // ─── Form state ───────────────────────────────────────────────────────
    var name by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf<SportDto?>(null) }
    var sportExpanded by remember { mutableStateOf(false) }
    var sportsList by remember { mutableStateOf<List<SportDto>>(emptyList()) }

    var date by remember { mutableStateOf("") }    // “YYYY-MM-DD”
    var time by remember { mutableStateOf("") }    // “HH:MM”

    var participants by remember { mutableStateOf("") }

    // Location field + autocomplete state:
    var locationInput by remember { mutableStateOf("") }
    var locationValid by remember { mutableStateOf(false) }
    var locationSuggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var locationDropdownVisible by remember { mutableStateOf(false) }

    var showDeleteDialog by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()

    // Initialize Google Places if not already
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context, "YOUR_GOOGLE_MAPS_API_KEY")
        }
    }
    val placesClient: PlacesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }

    /**
     * 1) In one LaunchedEffect, load:
     *    a) all sports → sportsList
     *    b) the event detail → split its date/time, max_participants, place, etc.
     */
    LaunchedEffect(eventId) {
        // 1a) load sports
        sportsList = try {
            api.getSports("Bearer $token")
        } catch (e: Exception) {
            emptyList()
        }

        // 1b) load detail of this event
        try {
            val eventDto = api.getEventDetail(eventId, "Bearer $token")
            name = eventDto.name

            // split “startsAt” into date + time
            val raw = eventDto.startsAt ?: ""
            if (raw.contains("T")) {
                val parts = raw.split("T")
                date = parts[0]                               // YYYY-MM-DD
                time = parts.getOrNull(1)?.take(5) ?: ""      // HH:MM
            } else if (raw.contains(" ")) {
                val parts = raw.split(" ")
                date = parts[0]
                time = parts.getOrNull(1)?.take(5) ?: ""
            } else {
                date = raw
                time = ""
            }

            participants = eventDto.max_participants.toString()

            // prefill sport
            selectedSport = sportsList.firstOrNull { it.name == eventDto.sport }

            // prefill locationInput (mark valid)
            locationInput = eventDto.place
            locationValid = true
        } catch (_: Exception) {
            // ignore
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Activity") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Place, contentDescription = "Back")
                    }
                },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding)
        ) {
            // ─── Activity Name ─────────────────────────────────────────────
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Activity Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // ─── Dropdown “Sport” ────────────────────────────────────────
            ExposedDropdownMenuBox(
                expanded = sportExpanded,
                onExpandedChange = { sportExpanded = !sportExpanded }
            ) {
                OutlinedTextField(
                    value = selectedSport?.name ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sportExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = sportExpanded,
                    onDismissRequest = { sportExpanded = false }
                ) {
                    if (sportsList.isEmpty()) {
                        DropdownMenuItem(
                            text = { Text("Loading sports…") },
                            onClick = { /* no-op */ },
                            enabled = false
                        )
                    } else {
                        sportsList.forEach { sDto ->
                            DropdownMenuItem(
                                text = { Text(sDto.name) },
                                onClick = {
                                    selectedSport = sDto
                                    sportExpanded = false
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // ─── Date Picker ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val dp = DatePickerDialog(
                            context,
                            { _: DatePicker, y, m, d ->
                                date = String.format("%04d-%02d-%02d", y, m + 1, d)
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        dp.show()
                    }
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date (YYYY-MM-DD)") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Pick date",
                            modifier = Modifier.clickable {
                                val dp2 = DatePickerDialog(
                                    context,
                                    { _, y, m, d ->
                                        date = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                dp2.show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            // ─── Time Picker ────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val tp = TimePickerDialog(
                            context,
                            { _, hourOfDay, minute ->
                                time = String.format("%02d:%02d", hourOfDay, minute)
                            },
                            calendar.get(Calendar.HOUR_OF_DAY),
                            calendar.get(Calendar.MINUTE),
                            true
                        )
                        tp.show()
                    }
            ) {
                OutlinedTextField(
                    value = time,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time (HH:MM)") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = "Pick time",
                            modifier = Modifier.clickable {
                                val tp2 = TimePickerDialog(
                                    context,
                                    { _, h, min ->
                                        time = String.format("%02d:%02d", h, min)
                                    },
                                    calendar.get(Calendar.HOUR_OF_DAY),
                                    calendar.get(Calendar.MINUTE),
                                    true
                                )
                                tp2.show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Spacer(Modifier.height(12.dp))

            // ─── Number of Participants ────────────────────────────────────
            OutlinedTextField(
                value = participants,
                onValueChange = { participants = it },
                label = { Text("Number of Participants") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Participants"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // ─── Location (Autocomplete: ANY place type) ───────────────────
            Column {
                OutlinedTextField(
                    value = locationInput,
                    onValueChange = {
                        locationInput = it
                        locationValid = false
                        locationDropdownVisible = true
                    },
                    label = { Text("Location") },
                    singleLine = true,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Location",
                            tint = if (locationValid) Color(0xFF00C853) else Color.Unspecified
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )

                if (locationDropdownVisible && locationSuggestions.isNotEmpty()) {
                    Card(
                        elevation = CardDefaults.cardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    ) {
                        LazyColumn {
                            items(locationSuggestions) { prediction ->
                                DropdownMenuItem(
                                    text = {
                                        Text(prediction.getFullText(null).toString())
                                    },
                                    onClick = {
                                        locationInput = prediction.getFullText(null).toString()
                                        locationValid = true
                                        locationDropdownVisible = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))

            // ─── SAVE CHANGES ────────────────────────────────────────────────
            Button(
                onClick = {
                    coroutineScope.launch {
                        if (!locationValid) {
                            // Show a friendly Snackbar warning
                            snackbarHostState.showSnackbar(
                                message = "Please select a valid place from the suggestions."
                            )
                            return@launch
                        }
                        try {
                            val dateTimeUtc = "$date $time:00"
                            val dto = EventUpdateRequest(
                                name = name.trim(),
                                sport_id = selectedSport?.id ?: 0,
                                date = dateTimeUtc,
                                place = locationInput.trim(),
                                max_participants = participants.toIntOrNull() ?: 0,
                            )
                            val resp = api.updateActivity(
                                token = "Bearer $token",
                                id = eventId,
                                updatedEvent = dto
                            )
                            if (resp.isSuccessful) {
                                // Navigate back (detail screen will refresh on viewModel init)
                                onSave()
                            } else {
                                snackbarHostState.showSnackbar(
                                    message = "Update failed: HTTP ${resp.code()}"
                                )
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(
                                message = "Error updating event: ${e.localizedMessage}"
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF023499),
                    contentColor = Color.White
                )
            ) {
                Text("Save Changes")
            }

            Spacer(Modifier.height(12.dp))

            // ─── DELETE ACTIVITY ─────────────────────────────────────────────
            OutlinedButton(
                onClick = { showDeleteDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCD1606)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCD1606).copy(alpha = 0.1f),
                    contentColor = Color(0xFFCD1606)
                )
            ) {
                Text("Delete Activity")
            }

            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    confirmButton = {},
                    dismissButton = {},
                    text = {
                        DeleteActivityDialog(
                            onCancel = { showDeleteDialog = false },
                            onDelete = {
                                showDeleteDialog = false
                                coroutineScope.launch {
                                    try {
                                        val resp = api.deleteActivity(
                                            token = "Bearer $token",
                                            id = eventId
                                        )
                                        if (resp.isSuccessful) {
                                            onDelete()
                                        } else {
                                            snackbarHostState.showSnackbar(
                                                "Delete failed: HTTP ${resp.code()}"
                                            )
                                        }
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar(
                                            "Error deleting: ${e.localizedMessage}"
                                        )
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }

    // ─── QUERY GOOGLE PLACES FOR LOCATION SUGGESTIONS ──────────────────────
    LaunchedEffect(locationInput) {
        if (locationInput.isNotBlank()) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                // no TypeFilter: allow street addresses, landmarks, etc.
                .setQuery(locationInput)
                .build()
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    locationSuggestions = response.autocompletePredictions
                }
                .addOnFailureListener {
                    locationSuggestions = emptyList()
                }
        } else {
            locationSuggestions = emptyList()
        }
    }
}
