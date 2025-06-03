// File: app/src/main/java/com/example/teamup/ui/components/ActivityCard.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.teamup.R
import com.example.teamup.data.domain.model.ActivityItem
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

@Composable
fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    // If bgBrush is non-null, draw gradient; otherwise fill with bgColor
    bgColor: Color = Color(0xFFE1DCEF),
    bgBrush: Brush? = null,
    // Two optional labels, shown below date/time if non-null
    labelCreator: String? = null,
    labelConcluded: String? = null
) {
    // 1) Choose sport icon:
    val sportName = activity.title.substringAfter(":").trim().lowercase(Locale.ROOT)
    val iconRes = when (sportName) {
        "volleyball"          -> R.drawable.voleyball
        "basketball"          -> R.drawable.basketball
        "cycling", "ciclismo" -> R.drawable.ciclismo
        "football"            -> R.drawable.football
        "futsal"              -> R.drawable.futsal
        "handball"            -> R.drawable.handball
        "surf"                -> R.drawable.surf
        "tennis"              -> R.drawable.tennis
        else                  -> R.drawable.football
    }

    // 2) Parse startsAt → datePart (YYYY-MM-DD) and timePart (HH:MM)
    val raw = activity.startsAt
    val (datePart, timePart) = parseDateTime(raw)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = if (bgBrush != null) {
            // If gradient is provided, make the card's container transparent
            CardDefaults.cardColors(containerColor = Color.Transparent)
        } else {
            CardDefaults.cardColors(containerColor = bgColor)
        },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (bgBrush != null) Modifier.background(bgBrush)
                    else Modifier.background(bgColor)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                // Sport icon in a circle
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 2.dp,
                    modifier = Modifier.size(40.dp)
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = "$sportName icon",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    // ─── Title ───────────────────────────────────────────
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // ─── Location ────────────────────────────────────────
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // ─── Date • Time ─────────────────────────────────────
                    Text(
                        text = buildString {
                            if (datePart.isNotBlank()) {
                                append(datePart)
                                if (timePart.isNotBlank()) {
                                    append("  •  ")
                                    append(timePart)
                                }
                            } else {
                                append("—")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )

                    // ─── Labels (below date/time) ───────────────────────
                    labelCreator?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                    labelConcluded?.let {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontStyle = FontStyle.Italic
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            // ─── Participants count (top-right) ───────────────────────
            Text(
                text = "${activity.participants}/${activity.maxParticipants}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
            )

            // ─── Chevron (bottom-right) ───────────────────────────────
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See Activity",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(20.dp)
            )
        }
    }
}

private fun parseDateTime(raw: String): Pair<String, String> {
    if (raw.isBlank()) return "" to ""

    // 1) Try full ISO (e.g. “2025-06-10T18:30:00Z”)
    try {
        val zdt = ZonedDateTime.parse(raw)
        val local = zdt.withZoneSameInstant(ZoneId.systemDefault())
        val date = local.toLocalDate().toString()
        val time = "%02d:%02d".format(local.hour, local.minute)
        return date to time
    } catch (_: DateTimeParseException) { }

    // 2) Try “yyyy-MM-dd HH:mm:ss”
    try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = LocalDateTime.parse(raw, fmt)
        val date = ldt.toLocalDate().toString()
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        return date to time
    } catch (_: DateTimeParseException) { }

    // 3) Fallback: return raw as date only
    return raw to ""
}
