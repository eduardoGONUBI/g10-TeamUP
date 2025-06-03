// File: app/src/main/java/com/example/teamup/ui/screens/Activity/CreatorActivityScreen.kt
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.model.ParticipantUi
import com.example.teamup.data.remote.model.StatusUpdateRequest
import com.example.teamup.data.remote.model.FeedbackRequestDto
import com.example.teamup.ui.components.ActivityInfoCard
import com.example.teamup.ui.model.ParticipantRow
import com.example.teamup.ui.screens.ActivityDetailViewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatorActivityScreen(
    eventId: Int,
    token: String,
    onBack: () -> Unit,
    onEdit: (eventId: Int) -> Unit,
    onUserClick: (userId: Int) -> Unit
) {
    // ─── 1) Instantiate the ViewModel ─────────────────────────────────
    val viewModel: ActivityDetailViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ActivityDetailViewModel(eventId, token) as T
        }
    )

    // ─── 2) Observe the event state ────────────────────────────────────
    val eventState by viewModel.event.collectAsState()
    val api       = remember { ActivityApi.create() }
    val scope     = rememberCoroutineScope()

    // ─── Loading spinner while null ────────────────────────────────────
    if (eventState == null) {
        Box(
            Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // ─── 3) Non-null event now available ───────────────────────────────
    val e           = eventState!!
    val isConcluded = (e.status != "in progress")

    // Build a list of ParticipantUi (with level info from ViewModel)
    val uiParts: List<ParticipantUi> = e.participants.orEmpty()
        .distinctBy { it.id }
        .map { dto ->
            ParticipantUi(
                id        = dto.id,
                name      = dto.name,
                isCreator = (dto.id == e.creator.id),
                level     = dto.level ?: 0
            )
        }

    // ─── 4) Track which participant‐IDs have already received feedback ───
    val sentFeedbackIds = remember { mutableStateListOf<Int>() }

    // ─── 5) When feedback is successfully submitted, show a confirmation popup ─
    var confirmedName by remember { mutableStateOf<String?>(null) }

    // ─── 6) Track which participant the user tapped “Give feedback” on ─────
    var feedbackTarget by remember { mutableStateOf<ParticipantUi?>(null) }
    var kickTarget     by remember { mutableStateOf<ParticipantUi?>(null) }
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
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                // Edit button
                IconButton(onClick = { onEdit(e.id) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                // Cancel (delete) button
                IconButton(onClick = {
                    scope.launch {
                        val resp = api.deleteActivity("Bearer $token", e.id)
                        if (resp.isSuccessful) {
                            onBack()
                        } else {
                            println("Cancel failed: ${resp.code()}")
                        }
                    }
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Cancel activity",
                        tint = Color.Red
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                // Conclude or Reopen
                if (e.status == "in progress") {
                    IconButton(onClick = {
                        scope.launch {
                            val resp = api.concludeByCreator("Bearer $token", e.id)
                            if (resp.isSuccessful) {
                                viewModel.fetchEventWithLevels()
                            } else {
                                println("Conclude failed: ${resp.code()}")
                            }
                        }
                    }) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = "Mark as concluded",
                            tint = Color(0xFF1E88E5) // Blue
                        )
                    }
                } else {
                    IconButton(onClick = {
                        scope.launch {
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
                            Icons.Default.Refresh,
                            contentDescription = "Re-open activity",
                            tint = Color(0xFFFFA000) // Orange
                        )
                    }
                }
            }
        )

        // ─── Body: Info card, Map, Participant list ────────────────────────
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
                    text = "Participants (${uiParts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // 4) ParticipantRow for each participant
            items(uiParts, key = { it.id }) { p ->
                ParticipantRow(
                    p            = p,
                    isKickable   = (!p.isCreator && !isConcluded),
                    onKickClick  = { /* same as before */ },
                    onClick      = { onUserClick(p.id) },
                    showFeedback = isConcluded && (p.id !in sentFeedbackIds),
                    onFeedback   = { feedbackTarget = p }
                )
            }
        }
    }

    // ─── Kick Confirmation Dialog (unchanged) ─────────────────────────
    kickTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { kickTarget = null },
            confirmButton = {
                TextButton(onClick = {
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
            text = { Text("Kick ${target.name} from this event?") }
        )
    }

    // ─── Feedback Dialog ──────────────────────────────────────────────
    feedbackTarget?.let { target ->
        FeedbackDialog(
            target = target,
            onDismiss = { feedbackTarget = null },
            onSubmitAttr = { attr ->
                feedbackTarget = null
                scope.launch {
                    // 1) Call the backend:
                    val resp: Response<Void> = AchievementsApi.create().giveFeedback(
                        "Bearer $token",
                        e.id,
                        FeedbackRequestDto(user_id = target.id, attribute = attr)
                    )

                    if (resp.isSuccessful) {
                        // 2) Mark this participant's ID as “feedback sent”
                        sentFeedbackIds.add(target.id)
                        // 3) Trigger the confirmation popup
                        confirmedName = target.name
                    } else {
                        // Optionally, show an error Snackbar or log
                        println("Feedback failed: ${resp.code()}")
                    }
                }
            }
        )
    }

    // ─── Confirmation AlertDialog ────────────────────────────────────
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


// ─────────────────────────────────────────────────────────────
// File: Feedback helpers (same as before)
// ─────────────────────────────────────────────────────────────

/**
 * Data class that matches what your Laravel backend expects:
 *   { "user_id": 42, "attribute": "good_teammate" }
 */
data class FeedbackRequestDto(
    val user_id: Int,
    val attribute: String
)

private val feedbackOptions = listOf(
    "good_teammate" to "✅ Good teammate",
    "friendly" to "😊 Friendly",
    "team_player" to "🤝 Team player",
    "toxic" to "⚠️ Toxic",
    "bad_sport" to "👎 Bad sport",
    "afk" to "🚶 No show"
)

@Composable
internal fun FeedbackDialog(
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
                feedbackOptions.forEach { (value, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = value }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (selected == value),
                            onClick  = { selected = value }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label)
                    }
                }
            }
        }
    )
}
