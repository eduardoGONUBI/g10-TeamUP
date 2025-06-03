// File: app/src/main/java/com/example/teamup/ui/screens/Activity/ViewerActivityScreen.kt
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.ParticipantUi
import com.example.teamup.ui.components.ActivityInfoCard
import com.example.teamup.ui.components.WeatherCard
import com.example.teamup.ui.model.ParticipantRow
import com.example.teamup.ui.screens.ActivityDetailViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onUserClick: (userId: Int) -> Unit
) {
    // 1) ViewModel for fetching event + levels
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ActivityDetailViewModel(eventId, token) as T
            }
        }
    )

    val eventState by viewModel.event.collectAsState()
    val api = remember { ActivityApi.create() }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show loading while eventState == null
    if (eventState == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val e: ActivityDto = eventState!!

    // Build ParticipantUi (with level)
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

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
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
                            overflow = TextOverflow.Ellipsis,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Back",
                                tint = Color.Black,
                                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                            )
                        }
                    },
                    actions = {
                        // JOIN button
                        IconButton(onClick = {
                            coroutineScope.launch {
                                val response = api.joinEvent("Bearer $token", e.id)
                                if (response.isSuccessful) {
                                    snackbarHostState.showSnackbar("Joined event!")
                                    onBack()
                                } else {
                                    snackbarHostState.showSnackbar("Join failed: ${response.code()}")
                                }
                            }
                        }) {
                            Icon(
                                imageVector = Icons.Default.ExitToApp,
                                contentDescription = "Join Event",
                                tint = Color.Blue,
                                modifier = Modifier.scale(scaleX = 1f, scaleY = 1f)
                            )
                        }
                    }
                )

                LazyColumn(
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Info card
                    item {
                        ActivityInfoCard(
                            activity = e,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Map
                    item {
                        val coords = LatLng(e.latitude, e.longitude)
                        val cameraState: CameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(coords, 15f)
                        }
                        LaunchedEffect(cameraState, coords) {
                            cameraState.position = CameraPosition.fromLatLngZoom(coords, 15f)
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
                                cameraPositionState = cameraState
                            ) {
                                Marker(
                                    state = MarkerState(position = coords),
                                    title = e.place
                                )
                            }
                        }
                    }

                    item {
                        WeatherCard(weather = e.weather)
                    }

                    // Participants header
                    item {
                        Text(
                            text = "Participants (${uiParticipants.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                        )
                    }

                    // Participant rows
                    items(uiParticipants, key = { it.id }) { p ->
                        ParticipantRow(
                            p = p,
                            isKickable = false,
                            onKickClick = {},
                            onClick = { onUserClick(p.id) }
                        )
                    }
                }
            }
        }
    }
}
