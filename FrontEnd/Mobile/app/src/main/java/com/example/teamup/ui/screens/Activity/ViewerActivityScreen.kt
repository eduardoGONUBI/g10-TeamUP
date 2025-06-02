// app/src/main/java/com/example/teamup/ui/screens/Activity/ViewerActivityScreen.kt
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
fun ViewerActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit
) {
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()

    // Snackbar host to display “Joined!” or error messages
    val snackbarHostState = remember { SnackbarHostState() }

    var event by remember { mutableStateOf<ActivityDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    // ─── Load the event via GET /api/events/{id} ─────────────────────────────────
    LaunchedEffect(eventId) {
        try {
            val dto = api.getEventDetail(eventId, "Bearer $token")
            event = dto
            error = null
        } catch (e: Exception) {
            error = "Event not found"
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                event == null && error == null -> {
                    // Still loading
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                error != null -> {
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        Text(
                            text = "Error: $error",
                            color = Color.Red,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    // event is non-null
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
                            title = {
                                Text(
                                    text = e.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            navigationIcon = {
                                IconButton(onClick = onBack) {
                                    Icon(
                                        painter = painterResource(R.drawable.arrow_back),
                                        contentDescription = "Back"
                                    )
                                }
                            },
                            actions = {
                                // ─── JOIN BUTTON ─────────────────────────────────────────────
                                IconButton(onClick = {
                                    coroutineScope.launch {
                                        try {
                                            val response = api.joinEvent("Bearer $token", e.id)
                                            if (response.isSuccessful) {
                                                // Show “Joined!” message, then pop back
                                                snackbarHostState.showSnackbar("Joined event!")
                                                onBack()
                                            } else {
                                                snackbarHostState.showSnackbar("Join failed: ${response.code()}")
                                            }
                                        } catch (ex: Exception) {
                                            snackbarHostState.showSnackbar("Join error: ${ex.localizedMessage}")
                                        }
                                    }
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Join Event",
                                        tint = Color.Blue,
                                        // Regular orientation (pointing “outward”)
                                        modifier = Modifier.scale(scaleX = 1f, scaleY = 1f)
                                    )
                                }
                            }
                        )

                        LazyColumn(
                            Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            item {
                                ActivityInfoCard(
                                    activity = e,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            item {
                                val coords = LatLng(e.latitude, e.longitude)
                                val camState = rememberCameraPositionState()
                                LaunchedEffect(camState, coords) {
                                    camState.position =
                                        CameraPosition.fromLatLngZoom(coords, 15f)
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
                                    text = "Participants (${uiParticipants.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier
                                        .padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
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
            }
        }
    }
}
