
package com.example.teamup.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.data.remote.model.ActivityDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.ui.graphics.Color

// detalhes de uma atividade
@Composable
fun ActivityInfoCard(
    activity: ActivityDto,
    modifier: Modifier = Modifier ,
    containerColor: Color = MaterialTheme.colorScheme.surface
) {

    val raw = activity.startsAt ?: ""
    val parsedInstant = runCatching { Instant.parse(raw) }.getOrNull()
    val zone = ZoneId.systemDefault()
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    val dateText: String = parsedInstant
        ?.atZone(zone)
        ?.format(dateFormatter)
        ?: raw.substringBefore('T').ifBlank { "–" }

    val timeText: String = parsedInstant?.atZone(zone)?.format(timeFormatter)
        ?: raw.substringAfter('T')
            .takeIf { raw.contains('T') }
            ?.substringBeforeLast(":", missingDelimiterValue = raw.substringAfter('T'))
            ?.take(5)
        ?: "–"

    Card(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Criador
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Organizer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = activity.creator.name,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 16.sp, fontWeight = FontWeight.SemiBold
                    )
                )
            }

            // Desporto
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SportsSoccer,
                    contentDescription = "Sport",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = activity.sport,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
            }

            // Data e hora
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                // data
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Date",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                }
                // hora
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = "Time",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                    )
                }
            }

            // location
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Place,
                    contentDescription = "Place",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = activity.place,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)
                )
            }

            // status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Status",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = activity.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 14.sp, fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}
