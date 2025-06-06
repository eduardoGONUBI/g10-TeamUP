// File: app/src/main/java/com/example/teamup/ui/model/ParticipantRow.kt
package com.example.teamup.ui.model

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.teamup.data.remote.model.ParticipantUi

/**
 * A single row in the “participants” list.
 *
 * @param p             The ParticipantUi (contains id, name, level, isCreator, etc.)
 * @param isConcluded   True if the event’s status is exactly "concluded"
 * @param isKickable    True if the current user (creator) can kick this participant right now
 * @param onKickClick   Called when the “Kick” button is tapped
 * @param onClick       Called when the user taps on the name/level to view that user’s profile
 * @param showFeedback  True if (1) event is concluded, (2) this participant has not yet received feedback, (3) this participant is not the creator
 * @param onFeedback    Called when “Give feedback” is tapped
 */
@Composable
fun ParticipantRow(
    p: ParticipantUi,
    isConcluded: Boolean,
    isKickable: Boolean,
    onKickClick: () -> Unit,
    onClick: () -> Unit,
    showFeedback: Boolean = false,
    onFeedback: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Clicking on the Column (name/level) should view that user’s profile:
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = p.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Lvl ${p.level}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            // 1) If this participant *is* the event’s creator, show a star icon (no feedback/kick)
            p.isCreator -> {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Creator",
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            // 2) If event is concluded, this user has not yet received feedback, and is not the creator:
            //    Show “Give feedback” button
            showFeedback && isConcluded -> {
                Button(
                    onClick = onFeedback,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text(
                        text = "Give feedback",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            // 3) If the event is not concluded AND the creator can still kick this participant:
            //    Show kick button
            isKickable && !isConcluded -> {
                IconButton(onClick = onKickClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kick Participant",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 4) If we reach here *and* the event is concluded (but no “showFeedback”):
            //    That means feedback was already sent → show “Feedback sent”
            isConcluded -> {
                Text(
                    text = "Feedback sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }

            // 5) Otherwise (event not concluded, not creator, not kickable, no feedback yet) → show nothing
            else -> {
                Spacer(modifier = Modifier.width(0.dp))
            }
        }
    }
}
