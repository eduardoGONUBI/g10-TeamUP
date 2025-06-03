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
 *   • Tapping the name/level opens the user’s profile (onClick).
 *   • When showFeedback == true, a “Give feedback” button appears.
 *   • Otherwise, if isKickable, show a kick‐icon.
 *   • If neither, display “Feedback sent”.
 */
@Composable
fun ParticipantRow(
    p: ParticipantUi,
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
        // Only this Column is clickable → view profile
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = p.name,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Lvl ${p.level}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        when {
            p.isCreator -> {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Creator",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            showFeedback -> {
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
            isKickable -> {
                IconButton(onClick = onKickClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kick Participant",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                Text(
                    text = "Feedback sent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
        }
    }
}
