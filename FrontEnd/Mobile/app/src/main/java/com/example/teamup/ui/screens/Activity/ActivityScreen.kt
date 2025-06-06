// File: app/src/main/java/com/example/teamup/ui/screens/Activity/ActivityScreen.kt
package com.example.teamup.ui.screens.Activity

import android.util.Base64
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
import org.json.JSONObject
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    eventId: Int,
    token: String,                    // raw JWT (no “Bearer ” prefix)
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
    // ─── 1) Shared ViewModel to load “ActivityDto + enriched participants” ─
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return ActivityDetailViewModel(eventId, token) as T
            }
        }
    )

    val eventState by viewModel.event.collectAsState()
    val api        = remember { ActivityApi.create() }
    val scope      = rememberCoroutineScope()

    // ─── 2) Show spinner until eventState != null ──────────────────────────
    if (eventState == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // ─── 3) Once loaded: extract ActivityDto ──────────────────────────────
    val e: ActivityDto = eventState!!

    // ─── 4) Compute “isConcluded” only if status == "concluded" ───────────
    val isConcluded = e.status.trim().equals("concluded", ignoreCase = true)

    // ─── 5) Parse currentUserId from JWT (same logic as ViewModel) ────────
    val currentUserId: Int? = remember(token) {
        try {
            val rawJwt = token.removePrefix("Bearer ").trim()
            val parts  = rawJwt.split(".")
            if (parts.size < 2) return@remember null
            val payloadBytes = Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP)
            val payloadJson  = JSONObject(String(payloadBytes, Charsets.UTF_8))
            payloadJson.getInt("sub")
        } catch (_: Exception) {
            null
        }
    }

    // ─── 6) Build ParticipantUi list with enriched levels ─────────────────
    val uiParticipants: List<ParticipantUi> = e.participants.orEmpty()
        .distinctBy { it.id }
        .map { dto ->
            ParticipantUi(
                id        = dto.id,
                name      = dto.name,
                isCreator = (dto.id == e.creator.id),
                level     = dto.level ?: 0
            )
        }

    // ─── 7) Track which participants already received feedback ─────────────
    val sentFeedbackIds = remember { mutableStateListOf<Int>() }
    var feedbackTarget   by remember { mutableStateOf<ParticipantUi?>(null) }
    var confirmedName    by remember { mutableStateOf<String?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var kickTarget       by remember { mutableStateOf<ParticipantUi?>(null) }

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
                            // ─── CREATOR: Edit / Cancel / Conclude / Reopen ───
                            onEdit?.let {
                                IconButton(onClick = it) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                                }
                            }
                            IconButton(onClick = { showCancelDialog = true }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Cancel activity",
                                    tint = Color.Red
                                )
                            }
                            Spacer(Modifier.width(8.dp))
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
                            // ─── PARTICIPANT: Only “Leave” ────────────────
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
                            // ─── VIEWER: Only “Join” ───────────────────
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
        // ─── 8) Shared body: Info card, Map, Weather, Participant list + Feedback ─────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 8a) Activity info
            item {
                ActivityInfoCard(
                    activity = e,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // 8b) Map
            item {
                val coords      = LatLng(e.latitude, e.longitude)
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

            // 8c) Weather
            item {
                WeatherCard(weather = e.weather, modifier = Modifier.padding(horizontal = 24.dp))
            }

            // 8d) Participant header
            item {
                Text(
                    text = "Participants (${uiParticipants.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // 8e) Participant rows
            items(uiParticipants, key = { it.id }) { p ->
                val thisIsKickable   = (role == ActivityRole.CREATOR && !isConcluded && !p.isCreator)
                val thisShowFeedback = (
                        isConcluded &&
                                (p.id !in sentFeedbackIds) &&
                                (p.id != currentUserId)
                        )

                ParticipantRow(
                    p            = p,
                    isConcluded  = isConcluded,
                    isKickable   = thisIsKickable,
                    onKickClick  = { kickTarget = p },
                    onClick      = { onUserClick?.invoke(p.id) },
                    showFeedback = thisShowFeedback,
                    onFeedback   = { feedbackTarget = p }
                )
            }
        }
    }

    // ─── 9) “Cancel” dialog (only for CREATOR) ─────────────────────────────────────
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

    // ─── 10) “Kick” dialog (only CREATOR) ─────────────────────────────────────────
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

    // ─── 11) “Feedback” dialog (only after isConcluded == true) ────────────────
    if (feedbackTarget != null && isConcluded) {
        val target = feedbackTarget!!
        FeedbackDialog(
            target    = target,
            eventId   = e.id,
            token     = token,
            onDismiss = { feedbackTarget = null },
            onSuccess = {
                // Only called when /feedback returned 2xx
                sentFeedbackIds.add(target.id)
                confirmedName = target.name
            }
        )
    }

    // ─── 12) Confirmation pop‐up (“Feedback sent”) ────────────────────────────
    if (confirmedName != null) {
        AlertDialog(
            onDismissRequest = { confirmedName = null },
            confirmButton = {
                TextButton(onClick = { confirmedName = null }) {
                    Text("OK")
                }
            },
            title = { Text("Feedback sent") },
            text  = { Text("Your feedback for “${confirmedName}” has been submitted.") }
        )
    }
}
