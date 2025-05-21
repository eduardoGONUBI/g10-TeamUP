package com.example.teamup.ui.screens.Ativity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.teamup.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditActivityScreen(
    onSave: () -> Unit,
    onDelete: () -> Unit
) {
    // 1) Your custom outline color
    val outlineColor = Color(0xFF575DFB)

    // 2) Copy the existing scheme but swap out its 'outline' slot
    val customScheme = MaterialTheme.colorScheme.copy(
        outline = outlineColor
    )

    // 3) Wrap your content in this custom MaterialTheme
    MaterialTheme(
        colorScheme = customScheme,
        typography = MaterialTheme.typography,
        shapes     = MaterialTheme.shapes
    ) {
        // — all your state and UI below unchanged —
        var name by remember { mutableStateOf("") }
        var sportExpanded by remember { mutableStateOf(false) }
        var sport by remember { mutableStateOf("") }
        val sports = listOf("Football", "Basketball", "Tennis", "Badminton", "Hockey")
        var date by remember { mutableStateOf("") }
        var time by remember { mutableStateOf("") }
        var participants by remember { mutableStateOf("") }
        var location by remember { mutableStateOf("") }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Activity Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Activity Name") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Sport
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

            // Date
            OutlinedTextField(
                value = date,
                onValueChange = { date = it },
                label = { Text("Date") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_calendar_month_24),
                        contentDescription = "Date picker"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Time
            OutlinedTextField(
                value = time,
                onValueChange = { time = it },
                label = { Text("Time") },
                trailingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.baseline_sports_soccer_24),
                        contentDescription = "Time picker"
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))

            // Number of Participants
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

            // Location
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

            // Save
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF023499),
                    contentColor   = Color.White
                )
            ) {
                Text("Save Changes")
            }
            Spacer(Modifier.height(12.dp))

            // Delete
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, Color(0xFFCD1606)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFCD1606).copy(alpha = 0.1f),
                    contentColor   = Color(0xFFCD1606)
                )
            ) {
                Text("Delete Activity")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun EditActivityScreenPreview() {
    EditActivityScreen(
        onSave   = { /* noop */ },
        onDelete = { /* noop */ }
    )
}
