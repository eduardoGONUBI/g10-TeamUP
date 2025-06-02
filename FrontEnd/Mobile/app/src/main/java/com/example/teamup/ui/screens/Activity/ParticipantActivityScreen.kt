// app/src/main/java/com/example/teamup/ui/screens/Activity/ParticipantActivityScreen.kt
package com.example.teamup.ui.screens.Activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.teamup.R
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.data.remote.ActivityDto
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit
) {
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()
    var event by remember { mutableStateOf<ActivityDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load via getEventDetail(...)
    LaunchedEffect(eventId) {
        try {
            val dto = api.getEventDetail(eventId, "Bearer $token")
            event = dto
            error = null
        } catch (e: Exception) {
            error = "Event not found"
        }
    }

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
                    Icon(
                        painterResource(R.drawable.arrow_back),
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // ─── LEAVE BUTTON ───────────────────────────────────────────
                IconButton(onClick = {
                    coroutineScope.launch {
                        try {
                            val response = api.leaveEvent(
                                token = "Bearer $token", id = e.id
                            )
                            if (response.isSuccessful) onBack()
                            else println("Leave failed: ${response.code()}")
                        } catch (ex: Exception) {
                            println("Leave error: ${ex.localizedMessage}")
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Leave Event",
                        tint = Color.Red,
                        // Flip horizontally so it points “inward”
                        modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                    )
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
                    isKickable = false,
                    onKickClick = {}
                )
            }
        }
    }
}
