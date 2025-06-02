// File: app/src/main/java/com/example/teamup/ui/model/ParticipantUi.kt
package com.example.teamup.ui.model

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.R

/**
 * Presentation-layer representation of a participant.
 *
 * @param id         User ID.
 * @param name       Display name.
 * @param isCreator  True if this user created the event.
 * @param level      Level fetched from the Achievements service.
 */
data class ParticipantUi(
    val id: Int,
    val name: String,
    val isCreator: Boolean,
    val level: Int
)

/*
* Single row showing a participant’s name + level + “creator star” or red delete button.
* (We’re re‐defining it here to keep this file self‐contained.)
*/
@Composable
public fun ParticipantRow(
    p: ParticipantUi,
    isKickable: Boolean,
    onKickClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // 1) Participant’s name
                Text(
                    text = p.name,
                    fontWeight = if (p.isCreator) androidx.compose.ui.text.font.FontWeight.Bold
                    else androidx.compose.ui.text.font.FontWeight.Normal,
                    fontSize = 16.sp
                )
                // 2) Participant’s level
                Text(
                    text = "Lvl ${p.level}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // 3) Star icon if creator, else red delete if kickable
            if (p.isCreator) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Creator",
                    tint = Color(0xFFFFC107), // gold
                    modifier = Modifier.size(18.dp)
                )
            } else if (isKickable) {
                IconButton(onClick = onKickClick) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Kick Participant",
                        tint = Color.Red
                    )
                }
            }
        }
        Divider(Modifier.padding(horizontal = 24.dp))
    }
}
/**
 * A small helper composable (in case you want label/value pairs elsewhere).
 * Not strictly required for the Creator screen, but included because you showed it earlier.
 */
@Composable
public fun Labeled(label: String, value: String, bold: Boolean = false) {
    Text(text = label, style = MaterialTheme.typography.labelMedium)
    Text(
        text = value,
        fontSize = if (bold) 20.sp else 16.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
    )
    Spacer(modifier = Modifier.height(12.dp))
}



