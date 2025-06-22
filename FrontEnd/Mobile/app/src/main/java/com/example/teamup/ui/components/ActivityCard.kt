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
import com.example.teamup.domain.model.Activity
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale

// cartao da atividade nas listas
@Composable
fun ActivityCard(
    activity: Activity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    bgColor: Color = Color(0xFFE1DCEF),
    bgBrush: Brush? = null,
    labelCreator: String? = null,
    labelConcluded: String? = null
) {
     // icons do desporto
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

    // parse data
    val raw = activity.startsAt
    val (datePart, timePart) = parseDateTime(raw)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = if (bgBrush != null) {
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

                    // ─── Labels ───────────────────────
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

            // ─── Participants ───────────────────────
            Text(
                text = "${activity.participants}/${activity.maxParticipants}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 8.dp, top = 8.dp)
            )

            // >>>>>>>>>
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

// parsa o start_at
private fun parseDateTime(raw: String): Pair<String, String> {
    if (raw.isBlank()) return "" to ""

    try {
        val zdt = ZonedDateTime.parse(raw)
        val local = zdt.withZoneSameInstant(ZoneId.systemDefault())
        val date = local.toLocalDate().toString()
        val time = "%02d:%02d".format(local.hour, local.minute)
        return date to time
    } catch (_: DateTimeParseException) { }

    try {
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val ldt = LocalDateTime.parse(raw, fmt)
        val date = ldt.toLocalDate().toString()
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        return date to time
    } catch (_: DateTimeParseException) { }

    return raw to ""
}
