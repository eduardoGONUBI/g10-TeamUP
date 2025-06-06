// File: app/src/main/java/com/example/teamup/ui/screens/Activity/ActivityScreen.kt
package com.example.teamup.ui.screens.Activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.model.ActivityDto
import com.example.teamup.data.remote.model.ParticipantUi
import com.example.teamup.data.remote.model.StatusUpdateRequest
import com.example.teamup.data.remote.model.FeedbackRequestDto
import com.example.teamup.ui.components.ActivityInfoCard
import com.example.teamup.ui.components.WeatherCard
import com.example.teamup.ui.model.ParticipantRow
import com.example.teamup.ui.popups.DeleteActivityDialog
import com.example.teamup.ui.popups.KickParticipantDialog
import com.example.teamup.ui.screens.ActivityDetailViewModel
import com.example.teamup.ui.screens.ActivityDetailViewModel.ActivityRole
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    eventId: Int,
    token: String,
    role: ActivityRole,
    onBack: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onJoin: (() -> Unit)? = null,
    onLeave: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onConclude: (() -> Unit)? = null,
    onReopen: (() -> Unit)? = null,
    onUserClick: ((Int) -> Unit)? = null
) {
    // ─── 1) Use the shared ViewModel to load “ActivityDto + enriched participants” ─────────
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ActivityDetailViewModel(eventId, token) as T
            }
        }
    )
    val eventState by viewModel.event.collectAsState()
    val api = remember { ActivityApi.create() }
    val scope = rememberCoroutineScope()

    // ─── 2) Show spinner until event is non‐null ─────────────────────────────────────────────
    if (eventState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val e: ActivityDto = eventState!!
    val isConcluded = (e.status != "in progress")

    // ─── 3) Build a list of ParticipantUi for display (with enriched levels) ───────────────
    val uiParticipants: List<ParticipantUi> = e.participants.orEmpty()
        .distinctBy { it.id }
        .map { dto ->
            ParticipantUi(
                id = dto.id,
                name = dto.name,
                isCreator = (dto.id == e.creator.id),
                level = dto.level ?: 0
            )
        }

    // ─── 4) Keep track of which participants have already been given feedback ───────────────
    val sentFeedbackIds = remember { mutableStateListOf<Int>() }
    var feedbackTarget by remember { mutableStateOf<ParticipantUi?>(null) }
    var confirmedName by remember { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var kickTarget by remember { mutableStateOf<ParticipantUi?>(null) }

    Scaffold(
        topBar = {
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    when (role) {
                        ActivityRole.CREATOR -> {
                            // ─── C R E A T O R : Edit / Cancel / Conclude / Reopen ───
                            // Edit button (if provided)
                            onEdit?.let {
                                IconButton(onClick = it) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                            // Cancel (delete) button
                            IconButton(onClick = { showCancelDialog = true }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel activity",
                                    tint = Color.Red
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            // Conclude or Reopen
                            if (!isConcluded) {
                                onConclude?.let { concludeLambda ->
                                    IconButton(onClick = concludeLambda) {
                                        Icon(
                                            Icons.Default.Done,
                                            contentDescription = "Conclude",
                                            tint = Color(0xFF1E88E5)
                                        )
                                    }
                                }
                            } else {
                                onReopen?.let { reopenLambda ->
                                    IconButton(onClick = reopenLambda) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Re-open",
                                            tint = Color(0xFFFFA000)
                                        )
                                    }
                                }
                            }
                        }

                        ActivityRole.PARTICIPANT -> {
                            // ─── P A R T I C I P A N T : Only “Leave” ─────────────────
                            onLeave?.let { leaveLambda ->
                                IconButton(onClick = leaveLambda) {
                                    Icon(
                                        imageVector = Icons.Default.ExitToApp,
                                        contentDescription = "Leave Event",
                                        tint = Color.Red,
                                        modifier = Modifier.scale(scaleX = -1f, scaleY = 1f)
                                    )
                                }
                            }
                        }

                        ActivityRole.VIEWER -> {
                            // ─── V I E W E R : Only “Join” ────────────────────────────
                            onJoin?.let { joinLambda ->
                                IconButton(onClick = joinLambda) {
                                    Icon(
                                        Icons.Default.ExitToApp,
                                        contentDescription = "Join Event",
                                        tint = Color.Blue,
                                        modifier = Modifier.scale(scaleX = 1f, scaleY = 1f)
                                    )
                                }
                            }
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        // ─── 5) Shared body: Info card, Map, Weather, Participant list + Feedback ─────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 5a) Activity info
            item {
                ActivityInfoCard(
                    activity = e,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 5b) Map
            item {
                val coords = LatLng(e.latitude, e.longitude)
                val cameraState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(coords, 15f)
                }
                LaunchedEffect(cameraState, coords) {
                    cameraState.position = CameraPosition.fromLatLngZoom(coords, 15f)
                }
                Card(
                    modifier = Modifier
                        .padding(horizontal = 24.dp, vertical = 8.dp)
                        .fillMaxWidth()
                        .height(220.dp),
                    elevation = CardDefaults.cardElevation(6.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraState
                    ) {
                        Marker(state = MarkerState(position = coords), title = e.place)
                    }
                }
            }

            // 5c) Weather
            item {
                WeatherCard(weather = e.weather, modifier = Modifier.padding(horizontal = 24.dp))
            }

            // 5d) Participant header
            item {
                Text(
                    text = "Participants (${uiParticipants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // 5e) Participant rows
            items(uiParticipants, key = { it.id }) { p ->
                ParticipantRow(
                    p = p,
                    isKickable = (role == ActivityRole.CREATOR && !isConcluded && !p.isCreator),
                    onKickClick = { kickTarget = p },
                    onClick = { onUserClick?.invoke(p.id) },
                    showFeedback = isConcluded && (p.id !in sentFeedbackIds),
                    onFeedback = { feedbackTarget = p }
                )
            }
        }
    }

    // ─── 6) “Cancel” dialog (only if CREATOR pressed “Cancel”) ────────────────────────────
    if (showCancelDialog && role == ActivityRole.CREATOR) {
        Dialog(onDismissRequest = { showCancelDialog = false }) {
            DeleteActivityDialog(
                onCancel = { showCancelDialog = false },
                onDelete = {
                    showCancelDialog = false
                    scope.launch {
                        val resp = api.deleteActivity("Bearer $token", e.id)
                        if (resp.isSuccessful) {
                            onCancel?.invoke()
                        } else {
                            println("Delete failed: ${resp.code()}")
                        }
                    }
                }
            )
        }
    }

    // ─── 7) “Kick” dialog (only CREATOR) ────────────────────────────────────────────────
    if (kickTarget != null && role == ActivityRole.CREATOR) {
        val target = kickTarget!!
        Dialog(onDismissRequest = { kickTarget = null }) {
            KickParticipantDialog(
                name = target.name,
                onCancel = { kickTarget = null },
                onKick = {
                    kickTarget = null
                    scope.launch {
                        val resp = api.kickParticipant(
                            token = "Bearer $token",
                            eventId = e.id,
                            participantId = target.id
                        )
                        if (resp.isSuccessful) {
                            viewModel.fetchEventWithLevels()
                        } else {
                            println("Kick failed: ${resp.code()}")
                        }
                    }
                }
            )
        }
    }

    // ─── 8) “Feedback” dialog (all roles, but only after concluded) ─────────────────────
    if (feedbackTarget != null && isConcluded) {
        val target = feedbackTarget!!
        FeedbackDialog(
            target = target,
            onDismiss = { feedbackTarget = null },
            onSubmitAttr = { attr ->
                feedbackTarget = null
                scope.launch {
                    val resp: Response<Void> = AchievementsApi.create().giveFeedback(
                        "Bearer $token",
                        e.id,
                        FeedbackRequestDto(user_id = target.id, attribute = attr)
                    )
                    if (resp.isSuccessful) {
                        sentFeedbackIds.add(target.id)
                        confirmedName = target.name
                    } else {
                        println("Feedback failed: ${resp.code()}")
                    }
                }
            }
        )
    }

    // ─── 9) Confirmation pop‐up (feedback sent) ──────────────────────────────────────────
    if (confirmedName != null) {
        AlertDialog(
            onDismissRequest = { confirmedName = null },
            confirmButton = {
                TextButton(onClick = { confirmedName = null }) {
                    Text("OK")
                }
            },
            title = { Text("Feedback sent") },
            text = { Text("Your feedback for “${confirmedName}” has been submitted.") }
        )
    }
}


// ─────────────────────────────────────────────────────────────
//  FeedbackDialog is the same as before
// ─────────────────────────────────────────────────────────────

@Composable
private fun FeedbackDialog(
    target: ParticipantUi,
    onDismiss: () -> Unit,
    onSubmitAttr: (String) -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = (selected != null),
                onClick = { selected?.let(onSubmitAttr) }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        title = { Text("Give feedback") },
        text = {
            Column {
                Text("Choose a badge for ${target.name}:")
                Spacer(Modifier.height(8.dp))

                val options = listOf(
                    "good_teammate" to "✅ Good teammate",
                    "friendly" to "😊 Friendly",
                    "team_player" to "🤝 Team player",
                    "toxic" to "⚠️ Toxic",
                    "bad_sport" to "👎 Bad sport",
                    "afk" to "🚶 No show"
                )

                options.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == value),
                            onClick = { selected = value }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        }
    )
}
