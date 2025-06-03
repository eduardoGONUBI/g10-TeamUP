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
    /* ─── 1) Background + icon resource ─────────────────────────────── */

    val cardBg        = Color(0xFFE1DCEF)        // lavender
    val sportName     = activity.title.substringAfter(":").trim().lowercase(Locale.ROOT)
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

    /* ─── 2) Parse date + time robustly  ────────────────────────────── */

    val raw = activity.date ?: ""
    val (datePart, timePart) = parseDateTime(raw)

    /* ─── 3) Card layout  ───────────────────────────────────────────── */

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(Modifier.fillMaxWidth()) {

            /* ---- Main row: icon ⟶ title / location / date•time -------- */
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Surface(
                    shape  = CircleShape,
                    color  = MaterialTheme.colorScheme.surface,
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

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = activity.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = activity.location ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text  = buildString {
                            if (datePart.isNotBlank()) {
                                append(datePart)
                                if (timePart.isNotBlank()) {
                                    append("  •  ")
                                    append(timePart)
                                }
                            } else append("—")
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            /* ---- Participants (top-right) & Chevron (bottom-right) ---- */
            Text(
                text     = "${activity.participants}/${activity.maxParticipants}",
                style    = MaterialTheme.typography.bodySmall,
                color    = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
            )

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "See Activity",
                tint     = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 12.dp, bottom = 12.dp)
                    .size(20.dp)
            )
        }
    }
}

/* ──────────────────────────────────────────────────────────────── */
/* Helper: turn any backend string into  Pair(YYYY-MM-DD, HH:MM)   */
/* ──────────────────────────────────────────────────────────────── */
private fun parseDateTime(raw: String): Pair<String, String> {
    if (raw.isBlank()) return "" to ""

    /* 1) Try full ISO offset (e.g. 2025-06-03T14:45:00.000000Z) */
    try {
        val zdt = ZonedDateTime.parse(raw)
        val local = zdt.withZoneSameInstant(ZoneId.systemDefault())
        return local.toLocalDate().toString() to "%02d:%02d".format(local.hour, local.minute)
    } catch (_: DateTimeParseException) { /* fall through */ }

    /* 2) Try “yyyy-MM-dd HH:mm:ss” */
    try {
        val ldt = LocalDateTime.parse(
            raw,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        )
        return ldt.toLocalDate().toString() to "%02d:%02d".format(ldt.hour, ldt.minute)
    } catch (_: DateTimeParseException) { /* fall through */ }

    /* 3) Only a date?  Then no time. */
    return raw to ""
}
