// File: app/src/main/java/com/example/teamup/ui/components/ActivityInfoCard.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.teamup.data.remote.model.ActivityDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Displays the core information for a single event:
 *   • Organizer (creator)
 *   • Sport name
 *   • Date / Time (parsed from ISO‐8601 string)
 *   • Place (address)
 *   • Status (In progress / Concluded)
 *
 * @param activity  The ActivityDto to render.
 * @param modifier  Additional Modifier for styling / padding.
 */
@Composable
fun ActivityInfoCard(
    activity: ActivityDto,
    modifier: Modifier = Modifier
) {
    // Ensure we never call Instant.parse on a null String
    val raw = activity.startsAt ?: ""
    val parsedInstant = runCatching { Instant.parse(raw) }.getOrNull()

    val zone = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // If parsing succeeded, format both date and time. Otherwise, fall back to string‐splitting.
    val dateText = parsedInstant
        ?.atZone(zone)
        ?.format(dateFormatter)
        ?: raw.substringBefore('T')

    // For time, if the raw string had a ‘T’, take the HH:mm portion; otherwise, show “–.”
    val timeText = if (parsedInstant != null) {
        parsedInstant.atZone(zone).format(timeFormatter)
    } else {
        raw.substringAfter('T')
            .takeIf { raw.contains('T') }
            ?.substringBeforeLast(":", missingDelimiterValue = raw.substringAfter('T'))
            ?.take(5)
            ?: "–"
    }

    Card(
        modifier = modifier
            .padding(24.dp)
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(6.dp)
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

            // Date & Time
            Row(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = dateText.ifBlank { "–" },
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
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Status
            Text(
                text = "Status",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = activity.status.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
