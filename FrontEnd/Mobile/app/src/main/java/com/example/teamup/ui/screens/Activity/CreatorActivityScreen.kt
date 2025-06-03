// File: app/src/main/java/com/example/teamup/ui/screens/Activity/CreatorActivityScreen.kt
package com.example.teamup.ui.screens.Activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.ParticipantUi
import com.example.teamup.data.remote.model.StatusUpdateRequest
import com.example.teamup.ui.components.ActivityInfoCard
import com.example.teamup.ui.model.ParticipantRow
import com.example.teamup.ui.screens.ActivityDetailViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

/**
 * Screen for the event creator. Shows event info, map, and a list of participants
 * (with their current level). Allows the creator to Conclude or Reopen the event.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onEdit: (eventId: Int) -> Unit,
    onUserClick: (userId: Int) -> Unit
) {
    // 1) Instantiate the ViewModel (this immediately fetches the event + participant levels)
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ActivityDetailViewModel(eventId, token) as T
            }
        }
    )

    // 2) Collect the StateFlow<ActivityDto?> from the ViewModel
    val eventState by viewModel.event.collectAsState()
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()

    // 3) If still loading (null), show a spinner and return early
    if (eventState == null) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 4) Now we have a non-null ActivityDto
    val e = eventState!!
    val isConcluded = e.status != "in progress"

    // 5) Build ParticipantUi list, mapping each ParticipantDto.level → ParticipantUi.level
    val uiParticipants = e.participants.orEmpty()
        .distinctBy { it.id }
        .map {
            ParticipantUi(
                id = it.id,
                name = it.name,
                isCreator = (it.id == e.creator.id),
                level = it.level ?: 0
            )
        }

    var kickTarget by remember { mutableStateOf<ParticipantUi?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = {
                Text(
                    text = e.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 20.sp
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                // ─── EDIT BUTTON ─────────────────────────────────────────────────
                IconButton(onClick = { onEdit(e.id) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit"
                    )
                }

                // ─── CANCEL (DELETE) BUTTON ─────────────────────────────────────
                IconButton(onClick = {
                    coroutineScope.launch {
                        val resp = api.deleteActivity("Bearer $token", e.id)
                        if (resp.isSuccessful) {
                            onBack()
                        } else {
                            println("Cancel failed: ${resp.code()}")
                        }
                    }
                }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel activity",
                        tint = Color.Red
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // ─── CONCLUDE or REOPEN ────────────────────────────────────────
                if (e.status == "in progress") {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val resp = api.concludeByCreator("Bearer $token", e.id)
                            if (resp.isSuccessful) {
                                viewModel.fetchEventWithLevels()
                            } else {
                                println("Conclude failed: ${resp.code()}")
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Done,
                            contentDescription = "Mark as concluded",
                            tint = Color(0xFF1E88E5) // Blue
                        )
                    }
                } else {
                    IconButton(onClick = {
                        coroutineScope.launch {
                            val body = StatusUpdateRequest(status = "in progress")
                            val resp = api.updateStatus("Bearer $token", e.id, body)
                            if (resp.isSuccessful) {
                                viewModel.fetchEventWithLevels()
                            } else {
                                println("Re-open failed: ${resp.code()}")
                            }
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Re-open activity",
                            tint = Color(0xFFFFA000) // Orange
                        )
                    }
                }
            }
        )

        // ────────────────────────────────────────────────────────
        // BODY: ActivityInfoCard, Map, Participants List
        // ────────────────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1) ActivityInfoCard
            item {
                ActivityInfoCard(
                    activity = e,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2) Google Map
            item {
                val coords = LatLng(e.latitude, e.longitude)
                val cameraState: CameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(coords, 15f)
                }
                LaunchedEffect(cameraState, coords) {
                    cameraState.position = CameraPosition.fromLatLngZoom(coords, 15f)
                }

                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp)
                        .fillMaxWidth()
                        .height(220.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState
                    ) {
                        Marker(
                            state = MarkerState(position = coords),
                            title = e.place
                        )
                    }
                }
            }

            // 3) Participants header
            item {
                Text(
                    text = "Participants (${uiParticipants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // 4) ParticipantRow for each participant
            items(uiParticipants, key = { it.id }) { p ->
                ParticipantRow(
                    p = p,
                    isKickable = !p.isCreator && !isConcluded,   // kick only if not concluded
                    onKickClick = { kickTarget = p },
                    onClick = { onUserClick(p.id) },
                    showFeedback = isConcluded,                   // show “Give feedback” when concluded
                    onFeedback = { onUserClick(p.id) }
                )
            }
        }
    }

    // ────────────────────────────────────────────────────────────────
    // KICK DIALOG (AlertDialog)
    // ────────────────────────────────────────────────────────────────
    if (kickTarget != null) {
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            confirmButton = {
                TextButton(onClick = {
                    val target = kickTarget!!
                    kickTarget = null
                    coroutineScope.launch {
                        val response = api.kickParticipant(
                            token = "Bearer $token",
                            eventId = e.id,
                            participantId = target.id
                        )
                        if (response.isSuccessful) {
                            // Refresh once someone is kicked
                            viewModel.fetchEventWithLevels()
                        } else {
                            println("Kick failed: ${response.code()}")
                        }
                    }
                }) {
                    Text("Kick")
                }
            },
            dismissButton = {
                TextButton(onClick = { kickTarget = null }) {
                    Text("Cancel")
                }
            },
            title = { Text("Remove participant") },
            text = { Text("Kick ${kickTarget!!.name} from this event?") }
        )
    }
}
