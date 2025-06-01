// app/src/main/java/com/example/teamup/ui/components/ActivityCard.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.data.domain.model.ActivityItem

@Composable
fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = activity.title,
                color = Color(0xFF023499),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${activity.participants}/${activity.maxParticipants} Participants",
                color = Color(0xFF023499),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Location: ${activity.location}",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Date: ${activity.date}",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text = "Organizer: ${activity.organizer}",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "See Activity",
                color = Color(0xFF023499),
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
        }
    }
}
