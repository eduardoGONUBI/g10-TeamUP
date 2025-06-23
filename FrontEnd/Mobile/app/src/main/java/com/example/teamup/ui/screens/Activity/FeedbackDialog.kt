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

// give feedback menu
@Composable
fun FeedbackDialog(
    target: ParticipantUi,
    eventId: Int,
    token: String,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    // lista dos feedbacks
    val options = listOf(
        "good_teammate" to "✅ Good teammate",
        "friendly"      to "😊 Friendly",
        "team_player"   to "🤝 Team player",
        "toxic"         to "⚠️ Toxic",
        "bad_sport"     to "👎 Bad sport",
        "afk"           to "🚶 No show"
    )

    // estados
    var selected     by remember { mutableStateOf<String?>(null) }
    var isLoading    by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope        = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = {
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
                                    userId   = target.id,
                                    attribute = attr
                                )
                            )
                            isLoading = false

                            if (resp.isSuccessful) {
                                onSuccess()
                                onDismiss()
                            } else {
                                errorMessage = "Failed to send feedback (code ${resp.code()})"
                            }
                        }
                    }
                }
            ) {
                if (isLoading) {
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
