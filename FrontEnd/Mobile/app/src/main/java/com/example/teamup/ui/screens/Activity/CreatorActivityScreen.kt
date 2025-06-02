package com.example.teamup.ui.screens.Activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.R
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.data.remote.ActivityDto
import com.example.teamup.ui.popups.KickParticipantDialog
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

public data class ParticipantUi(
    val id: Int,
    val name: String,
    val isCreator: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onEdit: (eventId: Int) -> Unit
) {
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()

    var event by remember { mutableStateOf<ActivityDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var kickTarget by remember { mutableStateOf<ParticipantUi?>(null) }

    // Load the event
    LaunchedEffect(eventId) {
        try {
            val mine = api.getMyActivities("Bearer $token")
            event = mine.find { it.id == eventId }
            error = if (event == null) "Event not found" else null
        } catch (e: Exception) {
            error = e.localizedMessage
        }
    }

    // Loading / error states
    when {
        event == null && error == null -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        error != null -> {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text(
                    "Error: $error",
                    color = Color.Red,
                    textAlign = TextAlign.Center
                )
            }
            return
        }
    }

    // Non-null event
    val e = event!!
    val uiParticipants = e.participants.orEmpty()
        .distinctBy { it.id }
        .map {
            ParticipantUi(
                id = it.id,
                name = it.name,
                isCreator = it.id == e.creator.id
            )
        }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(e.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.arrow_back), contentDescription = "Back")
                }
            },
            actions = {
                IconButton(onClick = { onEdit(e.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
            }
        )

        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item {
                Card(
                    Modifier
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Labeled("Organizer", e.creator.name, bold = true)
                        Labeled("Date", e.date)
                        Labeled("Place", e.place)
                    }
                }
            }

            item {
                val coords = LatLng(e.latitude, e.longitude)
                val camState = rememberCameraPositionState()
                LaunchedEffect(camState, coords) {
                    camState.position = CameraPosition.fromLatLngZoom(coords, 15f)
                }

                Card(
                    Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(220.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = camState
                    ) {
                        Marker(
                            state = MarkerState(position = coords),
                            title = e.place
                        )
                    }
                }
            }

            item {
                Text(
                    "Participants (${uiParticipants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(uiParticipants, key = { it.id }) { p ->
                ParticipantRow(
                    p = p,
                    isKickable = !p.isCreator,
                    onKickClick = { kickTarget = p }
                )
            }
        }
    }

    // Kick dialog
    if (kickTarget != null) {
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            confirmButton = {},
            dismissButton = {},
            text = {
                KickParticipantDialog(
                    name = kickTarget!!.name,
                    onCancel = { kickTarget = null },
                    onKick = {
                        val participantId = kickTarget!!.id
                        kickTarget = null

                        // Launch your suspend call in a coroutine, not in LaunchedEffect
                        coroutineScope.launch {
                            try {
                                val response = api.kickParticipant(
                                    token = "Bearer $token",
                                    eventId = e.id,
                                    participantId = participantId
                                )
                                if (response.isSuccessful) {
                                    // Remove the kicked participant from state
                                    event = event!!.copy(
                                        participants = event!!.participants
                                            ?.filterNot { it.id == participantId }
                                    )
                                } else {
                                    // TODO: show user-visible error
                                    println("Kick failed: ${response.code()}")
                                }
                            } catch (e: Exception) {
                                println("Kick error: ${e.localizedMessage}")
                            }
                        }
                    }
                )
            }
        )
    }
}

@Composable
public fun ParticipantRow(
    p: ParticipantUi,
    isKickable: Boolean,
    onKickClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            p.name,
            fontWeight = if (p.isCreator) FontWeight.Bold else FontWeight.Normal,
            fontSize = 16.sp,
            modifier = Modifier.weight(1f)
        )
        if (p.isCreator) {
            Icon(
                Icons.Default.Star,
                contentDescription = "Creator",
                tint = Color(0xFFFFC107),
                modifier = Modifier.size(18.dp)
            )
        } else if (isKickable) {
            IconButton(onClick = onKickClick) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Kick Participant",
                    tint = Color.Red
                )
            }
        }
    }
    Divider(Modifier.padding(horizontal = 24.dp))
}

@Composable
public fun Labeled(label: String, value: String, bold: Boolean = false) {
    Text(label, style = MaterialTheme.typography.labelMedium)
    Text(
        value,
        fontSize = if (bold) 20.sp else 16.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
    Spacer(Modifier.height(12.dp))
}
