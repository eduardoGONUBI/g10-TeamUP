// File: app/src/main/java/com/example/teamup/ui/components/AchievementsRow.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.teamup.data.remote.model.AchievementDto
import com.example.teamup.data.remote.BaseUrlProvider
import java.net.URI

@Composable
fun AchievementsRow(
    achievements: List<AchievementDto>,
    modifier: Modifier = Modifier
) {
    // Track which achievement (if any) was tapped:
    var selectedAchievement by remember { mutableStateOf<AchievementDto?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        if (achievements.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp), // slightly shorter placeholder
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No achievements unlocked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(achievements) { achievement ->
                    AchievementIcon(
                        achievement = achievement,
                        onClick = { selectedAchievement = achievement }
                    )
                }
            }
        }

        // Only show the description (no title) when tapped:
        selectedAchievement?.let { ach ->
            AlertDialog(
                onDismissRequest = { selectedAchievement = null },
                confirmButton = {
                    Button(onClick = { selectedAchievement = null }) {
                        Text("OK")
                    }
                },
                text = {
                    Text(
                        text = ach.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            )
        }
    }
}

@Composable
private fun AchievementIcon(
    achievement: AchievementDto,
    onClick: (AchievementDto) -> Unit
) {
    // 1) Grab whatever "icon" URL backend gave us:
    var rawUrl = achievement.icon

    // 2) Strip out any "127.0.0.1:8000" → "10.0.2.2:8085" hack (emulator)
    if (rawUrl.contains("127.0.0.1:8000")) {
        rawUrl = rawUrl.replace("127.0.0.1:8000", "10.0.2.2:8085")
    }

    // 3) Extract only the path portion ("/achievements/XYZ.png"):
    val path = try {
        URI(rawUrl).path
    } catch (e: Exception) {
        rawUrl.substringAfter("/", "/")
    }

    // 4) Build final URL = BASE + path:
    val base    = BaseUrlProvider.getBaseUrl().trimEnd('/')
    val iconUrl = "$base$path"

    // Now render a smaller card:
    Card(
        modifier = Modifier
            .size(48.dp)               // ↓ smaller than 64dp
            .clickable { onClick(achievement) },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant // ↓ a subtler background
        )
    ) {
        // We shave off a little padding so the icon occupies more of the 48dp:
        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)         // ↓ less padding (was 8dp)
        )
    }
}
