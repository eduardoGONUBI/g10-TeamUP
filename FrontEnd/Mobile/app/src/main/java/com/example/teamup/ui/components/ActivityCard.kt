// File: app/src/main/java/com/example/teamup/ui/components/ActivityCard.kt
package com.example.teamup.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
    onClick: () -> Unit
) {
    // ─── 1) Background color + sport icon ───────────────────────────────
    val cardBg = Color(0xFFE1DCEF) // a soft lavender
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

    // ─── 2) Parse “startsAt” into a date + time string ───────────────────
    //    e.g. "2025-06-05T18:30:00Z" or "2025-06-05 18:30:00"
    val raw = activity.startsAt
    val (datePart, timePart) = parseDateTime(raw)

    // ─── 3) Build the card ─────────────────────────────────────────────
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // ---- Main row: icon → title / location / date•time ----
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
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
                    // Title (e.g. "Futsal : NEWDATE")
                    Text(
                        text = activity.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Location
                    Text(
                        text = activity.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // Date • Time (only show the “• Time” if timePart is not blank)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ---- Participants count (top-right) ----
            Text(
                text = "${activity.participants}/${activity.maxParticipants}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
            )

            // ---- Chevron (bottom-right) ----
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See Activity",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(20.dp)
            )
        }
    }
}

/**
 * Helper: given any back-end string, return Pair(YYYY-MM-DD, HH:MM).
 *
 * Tries three patterns in order:
 *  1) Full ISO (e.g. 2025-06-03T14:45:00.000000Z)
 *  2) Local pattern "yyyy-MM-dd HH:mm:ss"
 *  3) Fallback: treat input as a date only (no time).
 */
private fun parseDateTime(raw: String): Pair<String, String> {
    if (raw.isBlank()) return "" to ""

    /* 1) Try full ISO offset first */
    try {
        val zdt = ZonedDateTime.parse(raw)
        val local = zdt.withZoneSameInstant(ZoneId.systemDefault())
        val date = local.toLocalDate().toString()
        val time = "%02d:%02d".format(local.hour, local.minute)
        return date to time
    } catch (_: DateTimeParseException) {
        // fall through to next pattern
    }

    /* 2) Try “yyyy-MM-dd HH:mm:ss” (no timezone) */
    try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = LocalDateTime.parse(raw, fmt)
        val date = ldt.toLocalDate().toString()
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        return date to time
    } catch (_: DateTimeParseException) {
        // fall through
    }

    /* 3) If still not parsed, treat entire string as “just a date” */
    return raw to ""
}
