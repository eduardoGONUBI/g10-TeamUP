// File: app/src/main/java/com/example/teamup/ui/screens/Activity/ActivityInfoCard.kt
package com.example.teamup.ui.screens.Activity

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teamup.data.remote.ActivityDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A reusable card that displays:
 *   • Organizer
 *   • Sport
 *   • Date (YYYY-MM-DD)
 *   • Time (HH:MM)
 *   • Place
 *
 * It accepts a full ActivityDto and pulls out both e.date (ISO-8601) and e.sport, e.place, etc.
 */
@Composable
fun ActivityInfoCard(
    activity: ActivityDto,
    modifier: Modifier = Modifier
) {
    // 1) Try to parse the ISO-8601 timestamp (e.g. "2025-05-31T00:00:00.000000Z")
    //    into a java.time object, then reformat. If parsing fails, just show raw string.
    val parsedInstant = runCatching { Instant.parse(activity.date) }.getOrNull()
    val zone = ZoneId.systemDefault()

    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val dateText: String = parsedInstant
        ?.atZone(zone)
        ?.format(dateFormatter)
        ?: activity.date.takeWhile { it != 'T' } // fallback: everything before 'T'

    val timeText: String = parsedInstant
        ?.atZone(zone)
        ?.format(timeFormatter)
        ?: activity.date
            .substringAfter('T', "")
            .take(5) // fallback: HH:MM from "HH:MM:SS..."

    Card(
        modifier = modifier
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Organizer
            Text(
                text = "Organizer",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.creator.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Sport
            Text(
                text = "Sport",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.sport,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Date & Time (side by side)
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }
            }

            // Place
            Text(
                text = "Place",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.place,
                style = MaterialTheme.typography.bodyMedium
            )
            Labeled(
                label = "Status",
                value = activity.status.replaceFirstChar { it.uppercase() },   // → "In progress"
                bold  = true
            )
        }
    }
}
