// app/src/main/java/com/example/teamup/ui/screens/Activity/EditActivityScreen.kt
package com.example.teamup.ui.screens.Activity

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.teamup.data.remote.EventUpdateRequest
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.data.remote.SportDto
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

    // Custom outline color just for these text fields:
    val outlineColor = Color(0xFF575DFB)
    val customScheme = MaterialTheme.colorScheme.copy(outline = outlineColor)

    // ─── Form state ───────────────────────────────────────────────────────
    var name by remember { mutableStateOf("") }
    var selectedSport by remember { mutableStateOf<SportDto?>(null) }
    var sportExpanded by remember { mutableStateOf(false) }
    var sportsList by remember { mutableStateOf<List<SportDto>>(emptyList()) }

    var date by remember { mutableStateOf("") }    // “YYYY-MM-DD”
    var time by remember { mutableStateOf("") }    // “HH:MM”

    var participants by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    /**
     * 1) In one LaunchedEffect, load:
     *    a) all sports → sportsList
     *    b) the event detail → split its date/time, max_participants, place, etc.
     */
    LaunchedEffect(eventId) {
        // 1a) load sports
        try {
            sportsList = api.getSports("Bearer $token")
        } catch (e: Exception) {
            println("Falha ao carregar esportes: ${e.localizedMessage}")
        }

        // 1b) load detail of this event
        try {
            val eventDto = api.getEventDetail(eventId, "Bearer $token")

            name = eventDto.name
            location = eventDto.place

            // split the server’s ISO timestamp into “date” + “time”
            val raw = eventDto.date
            if (raw.contains("T")) {
                val parts = raw.split("T")
                date = parts[0]                                    // “YYYY-MM-DD”
                time = parts[1].substring(0, 5)                    // “HH:MM”
            } else {
                date = raw
                time = ""
            }

            participants = eventDto.max_participants.toString()

            // once sportsList is loaded, pick the matching SportDto by name:
            selectedSport = sportsList.firstOrNull { it.name == eventDto.sport }
        } catch (e: Exception) {
            println("Falha ao buscar detalhes do evento: ${e.localizedMessage}")
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
                    onValueChange = {},  // readOnly
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
                            { _, year, month, dayOfMonth ->
                                date = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
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

            // ─── Location ───────────────────────────────────────────────────
            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text("Location") },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Location"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // ─── SAVE CHANGES ────────────────────────────────────────────────
            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            // recombine “date + time” → "YYYY-MM-DD HH:MM:00"
                            val dateTimeUtc = "$date $time:00"
                            val dto = EventUpdateRequest(
                                name = name.trim(),
                                sport_id = selectedSport?.id ?: 0,
                                date = dateTimeUtc,
                                place = location.trim(),
                                max_participants = participants.toIntOrNull() ?: 0,
                            )
                            val resp = api.updateActivity(
                                token = "Bearer $token",
                                id = eventId,
                                updatedEvent = dto
                            )
                            if (resp.isSuccessful) {
                                onSave()
                            } else {
                                println("Update falhou: HTTP ${resp.code()}")
                            }
                        } catch (e: Exception) {
                            println("Erro ao atualizar evento: ${e.localizedMessage}")
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
                                            println("Delete falhou: HTTP ${resp.code()}")
                                        }
                                    } catch (e: Exception) {
                                        println("Erro ao deletar evento: ${e.localizedMessage}")
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
