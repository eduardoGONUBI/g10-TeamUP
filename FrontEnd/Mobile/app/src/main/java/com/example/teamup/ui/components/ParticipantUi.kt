// File: app/src/main/java/com/example/teamup/ui/model/ParticipantRow.kt
package com.example.teamup.ui.model

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A single row in the “participants” list.  Now exposes two callbacks:
 *   - onKickClick
 *   - onClick        ⟶ to view that user’s profile
 */
data class ParticipantUi(
    val id: Int,
    val name: String,
    val isCreator: Boolean,
    val level: Int
)

@Composable
fun ParticipantRow(
    p: ParticipantUi,
    isKickable: Boolean,
    onKickClick: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)  // tap target navigates to user profile
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
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

        if (p.isCreator) {
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "Creator",
                tint = MaterialTheme.colorScheme.primary
            )
        } else if (isKickable) {
            IconButton(onClick = onKickClick) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Kick Participant",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
