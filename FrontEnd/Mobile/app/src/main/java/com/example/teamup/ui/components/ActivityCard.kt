// app/src/main/java/com/example/teamup/ui/components/ActivityCard.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.teamup.data.domain.model.ActivityItem

@Composable
fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // We always use a white background here;
    // if you want conditional coloring (e.g. creator highlight), pass it in via a parameter instead.
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
                .padding(16.dp)
        ) {
            // ── Top row: Title on left, Chevron icon on right ─────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = activity.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF023499)
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Go to details",
                    tint = Color(0xFF023499)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Location: ${activity.location}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF023499)
            )
            Text(
                text = "Date: ${activity.date}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF023499)
            )
            Text(
                text = "Organizer: ${activity.organizer}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF023499)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${activity.participants}/${activity.maxParticipants} Participants",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF023499)
            )
        }
    }
}
