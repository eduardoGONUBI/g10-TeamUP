// File: app/src/main/java/com/example/teamup/ui/screens/Activity/ParticipantActivityScreen.kt
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
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.ParticipantUi
import com.example.teamup.data.remote.model.FeedbackRequestDto
import com.example.teamup.ui.components.ActivityInfoCard
import com.example.teamup.ui.components.WeatherCard
import com.example.teamup.ui.model.ParticipantRow
import com.example.teamup.ui.screens.ActivityDetailViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParticipantActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onUserClick: (userId: Int) -> Unit
) {
    // ─── 1) Load event + levels via ViewModel ─────────────────────────
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ActivityDetailViewModel(eventId, token) as T
        }
    )
    val eventState by viewModel.event.collectAsState()
    val api       = remember { ActivityApi.create() }
    val scope     = rememberCoroutineScope()

    // ─── 2) Show loading spinner until null ──────────────────────────
    if (eventState == null) {
        Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ─── 3) Non-null event retrieved ─────────────────────────────────
    val e           = eventState!!
    val isConcluded = (e.status != "in progress")
    val uiParts: List<ParticipantUi> = e.participants.orEmpty()
        .distinctBy { it.id }
        .map { ParticipantUi(it.id, it.name, it.id == e.creator.id, it.level ?: 0) }

    // ─── 4) Track which participant‐IDs have already received feedback ─
    val sentFeedbackIds = remember { mutableStateListOf<Int>() }

    // ─── 5) Which participant is being rated right now? ───────────────
    var feedbackTarget by remember { mutableStateOf<ParticipantUi?>(null) }

    // ─── 6) Which name to show in confirmation? ───────────────────────
    var confirmedName by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // ─── TopAppBar ────────────────────────────────────────────────
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
                        Icons.Default.ExitToApp,
                        contentDescription = "Back",
                        tint = Color.Black,
                        modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                    )
                }
            },
            actions = {
                // LEAVE button
                IconButton(onClick = {
                    scope.launch {
                        val resp = api.leaveEvent("Bearer $token", e.id)
                        if (resp.isSuccessful) onBack()
                        else println("Leave failed: ${resp.code()}")
                    }
                }) {
                    Icon(
                        Icons.Default.ExitToApp,
                        contentDescription = "Leave Event",
                        tint = Color.Red,
                        modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                    )
                }
            }
        )

        // ─── LazyColumn with Info card, Map, Participant list ─────────┐
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1) Info card
            item {
                ActivityInfoCard(
                    activity = e,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 2) Map block
            item {
                val coords = LatLng(e.latitude, e.longitude)
                val camState: CameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(coords, 15f)
                }
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
                WeatherCard(weather = e.weather)
            }

            // 3) Participants header
            item {
                Text(
                    text = "Participants (${uiParts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // 4) Participant rows
            items(uiParts, key = { it.id }) { p ->
                ParticipantRow(
                    p            = p,
                    isKickable   = false,
                    onKickClick  = { /* no‐op here for participants */ },
                    onClick      = { onUserClick(p.id) },
                    showFeedback = isConcluded && (p.id !in sentFeedbackIds),
                    onFeedback   = { feedbackTarget = p }
                )
            }
        }
    }

    // ─── Feedback Dialog ─────────────────────────────────────────────
    feedbackTarget?.let { target ->
        FeedbackDialog(
            target = target,
            onDismiss = { feedbackTarget = null },
            onSubmitAttr = { attr ->
                feedbackTarget = null
                scope.launch {
                    // 1) Call the backend to store feedback
                    val resp: Response<Void> = AchievementsApi.create().giveFeedback(
                        "Bearer $token",
                        e.id,
                        FeedbackRequestDto(user_id = target.id, attribute = attr)
                    )

                    if (resp.isSuccessful) {
                        // 2) Add this participant ID to “already sent” list
                        sentFeedbackIds.add(target.id)
                        // 3) Trigger confirmation pop‐up
                        confirmedName = target.name
                    } else {
                        // Optionally: show an error Snackbar or log
                        println("Feedback failed: ${resp.code()}")
                    }
                }
            }
        )
    }

    // ─── Confirmation pop‐up ─────────────────────────────────────────
    confirmedName?.let { name ->
        AlertDialog(
            onDismissRequest = { confirmedName = null },
            confirmButton = {
                TextButton(onClick = { confirmedName = null }) {
                    Text("OK")
                }
            },
            title = { Text("Feedback sent") },
            text = { Text("Your feedback for \"$name\" has been submitted.") }
        )
    }
}
