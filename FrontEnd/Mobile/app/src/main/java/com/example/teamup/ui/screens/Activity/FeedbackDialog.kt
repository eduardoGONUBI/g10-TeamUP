// File: app/src/main/java/com/example/teamup/ui/screens/Activity/FeedbackDialog.kt
package com.example.teamup.ui.screens.Activity

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.teamup.data.remote.api.AchievementsApi
import com.example.teamup.data.remote.model.FeedbackRequestDto
import com.example.teamup.data.remote.model.ParticipantUi
import kotlinx.coroutines.launch
import retrofit2.Response

/**
 * A Dialog that (1) lets the user choose one “badge” and then (2) calls the
 * POST /api/events/{event_id}/feedback endpoint.  It only closes itself
 * once the server returns success (2xx).  Any non‐2xx sets `errorMessage`.
 *
 * @param target      The ParticipantUi we’re giving feedback to.
 * @param eventId     The numeric ID of the event (used in the path).
 * @param token       The raw JWT string (no “Bearer ” prefix).  Inside we do "Bearer $token".
 * @param onDismiss   Called whenever the dialog should disappear (either user‐cancel or after success).
 * @param onSuccess   Called only if the POST /feedback returned HTTP 2xx.  Usually used to add to sentFeedbackIds & show the “confirmation” alert.
 */
@Composable
fun FeedbackDialog(
    target: ParticipantUi,
    eventId: Int,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    // 1) List of all possible badge options:
    val options = listOf(
        "good_teammate" to "✅ Good teammate",
        "friendly"      to "😊 Friendly",
        "team_player"   to "🤝 Team player",
        "toxic"         to "⚠️ Toxic",
        "bad_sport"     to "👎 Bad sport",
        "afk"           to "🚶 No show"
    )

    var selected     by remember { mutableStateOf<String?>(null) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope        = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
            // Only allow dismiss if we’re not in the middle of a network call
            if (!isLoading) {
                onDismiss()
            }
        },
        confirmButton = {
            TextButton(
                enabled = (selected != null && !isLoading),
                onClick = {
                    selected?.let { attr ->
                        isLoading = true
                        errorMessage = null

                        scope.launch {
                            val resp: Response<Void> = AchievementsApi.create().giveFeedback(
                                "Bearer $token",
                                eventId,
                                FeedbackRequestDto(
                                    user_id   = target.id,
                                    attribute = attr
                                )
                            )
                            isLoading = false

                            if (resp.isSuccessful) {
                                // Only fire onSuccess if we got a 2xx
                                onSuccess()
                                onDismiss()
                            } else {
                                // Show the error code (e.g. 404)
                                errorMessage = "Failed to send feedback (code ${resp.code()})"
                            }
                        }
                    }
                }
            ) {
                if (isLoading) {
                    // Show a tiny spinner + “Submitting…”
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(16.dp)
                            .padding(end = 8.dp),
                        strokeWidth = 2.dp
                    )
                    Text("Submitting…", style = MaterialTheme.typography.bodySmall)
                } else {
                    Text("Submit")
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isLoading,
                onClick = { onDismiss() }
            ) {
                Text("Cancel")
            }
        },
        title = {
            Text(text = "Give feedback for “${target.name}”")
        },
        text = {
            Column {
                options.forEach { (value, label) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(enabled = !isLoading) { selected = value }
                            .padding(vertical = 6.dp)
                    ) {
                        RadioButton(
                            selected = (selected == value),
                            onClick = {
                                if (!isLoading) {
                                    selected = value
                                }
                            },
                            enabled = !isLoading
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text  = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    )
}
