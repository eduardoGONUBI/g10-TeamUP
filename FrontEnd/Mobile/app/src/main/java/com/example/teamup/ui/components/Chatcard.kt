// File: app/src/main/java/com/example/teamup/ui/components/ChatCard.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import com.example.teamup.R
import com.example.teamup.data.domain.model.ChatItem
import java.util.Locale

@Composable
fun ChatCard(
    chat: ChatItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // 1) Decide background color
    val bgColor = when {
        chat.isCreator     -> Color(0xFFE3F2FD)   // pale blue if creator
        chat.isParticipant -> Color(0xFFF5F5F5)   // light gray if participant
        else               -> Color(0xFFF5F5F5)   // light gray otherwise
    }

    // 2) Force all text/icon to black
    val titleColor         = Color.Black
    val secondaryTextColor = Color.Black.copy(alpha = 0.8f)
    val labelColor         = Color.Black.copy(alpha = 0.7f)
    val iconTint           = Color.Black

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors    = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 3) Sport→icon lookup
            val sportKey = chat.sport.trim().lowercase(Locale.ROOT)
            val iconRes = when (sportKey) {
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

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                modifier = Modifier.size(40.dp)
            ) {
                Image(
                    painter = painterResource(id = iconRes),
                    contentDescription = "${chat.sport} icon",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // 4) Text column with “You are the creator” BELOW sport
            Column(Modifier.weight(1f)) {
                // Title
                Text(
                    text = chat.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = titleColor
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Sport name
                Text(
                    text = chat.sport,
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryTextColor
                )

                // “You are the creator” label now comes below the sport
                if (chat.isCreator) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "You are the creator",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                        color = labelColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            // 5) Chevron icon
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = iconTint
            )
        }
    }
}
