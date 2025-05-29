package com.example.teamup.ui.screens.Ativity

import android.app.DatePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.teamup.R
import com.example.teamup.data.remote.EventUpdateRequest
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.ui.popups.DeleteActivityDialog
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()

    val outlineColor = Color(0xFF575DFB)
    val customScheme = MaterialTheme.colorScheme.copy(outline = outlineColor)

    var name by remember { mutableStateOf("") }
    var sportExpanded by remember { mutableStateOf(false) }
    var sport by remember { mutableStateOf("") }
    val sports = listOf("Football", "Basketball", "Tennis", "Badminton", "Hockey")
    var date by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Load event data once
    LaunchedEffect(eventId) {
        try {
            val events = api.getMyActivities("Bearer $token")
            val event = events.find { it.id == eventId }

            event?.let {
                name = it.name
                sport = it.sport
                date = it.date
                participants = it.max_participants.toString()
                location = it.place
            }
        } catch (e: Exception) {
            println("Failed to fetch event: ${e.localizedMessage}")
        }
    }

    MaterialTheme(
        colorScheme = customScheme,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Activity Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            ExposedDropdownMenuBox(
                expanded = sportExpanded,
                onExpandedChange = { sportExpanded = !sportExpanded }
            ) {
                OutlinedTextField(
                    value = sport,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Sport") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = sportExpanded,
                    onDismissRequest = { sportExpanded = false }
                ) {
                    sports.forEach { s ->
                        DropdownMenuItem(
                            text = { Text(s) },
                            onClick = {
                                sport = s
                                sportExpanded = false
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val datePickerDialog = DatePickerDialog(
                            context,
                            { _, year, month, dayOfMonth ->
                                val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                date = formattedDate
                            },
                            calendar.get(Calendar.YEAR),
                            calendar.get(Calendar.MONTH),
                            calendar.get(Calendar.DAY_OF_MONTH)
                        )
                        datePickerDialog.show()
                    }
            ) {
                OutlinedTextField(
                    value = date,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.baseline_calendar_month_24),
                            contentDescription = "Date picker",
                            modifier = Modifier.clickable {
                                val datePickerDialog = DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                        date = formattedDate
                                    },
                                    calendar.get(Calendar.YEAR),
                                    calendar.get(Calendar.MONTH),
                                    calendar.get(Calendar.DAY_OF_MONTH)
                                )
                                datePickerDialog.show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = participants,
                onValueChange = { participants = it },
                label = { Text("Number of Participants") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_person_24),
                        contentDescription = "Participants"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_sports_soccer_24),
                        contentDescription = "Location picker"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            val updatedEvent = EventUpdateRequest(
                                name = name,
                                sport = sport,
                                date = date,
                                max_participants = participants.toIntOrNull() ?: 0,
                                place = location
                            )

                            val response = api.updateActivity(
                                token = "Bearer $token",
                                id = eventId,
                                updatedEvent = updatedEvent
                            )

                            if (response.isSuccessful) {
                                onSave()
                            } else {
                                println("Update failed: ${response.code()}")
                            }
                        } catch (e: Exception) {
                            println("Error updating event: ${e.localizedMessage}")
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

            // Show Delete Dialog
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
                                        val response = api.deleteActivity(
                                            token = "Bearer $token",
                                            id = eventId
                                        )
                                        if (response.isSuccessful) {
                                            onDelete()
                                        } else {
                                            println("Delete failed: ${response.code()}")
                                        }
                                    } catch (e: Exception) {
                                        println("Error deleting event: ${e.localizedMessage}")
                                    }
                                }
                            }
                        )
                    }
                )
            }
        }
    }
}
