// File: app/src/main/java/com/example/teamup/ui/screens/Profile/PublicProfileScreen.kt
package com.example.teamup.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.teamup.presentation.profile.PublicProfileViewModel
import com.example.teamup.ui.components.AchievementsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    token: String,
    userId: Int,
    onBack: () -> Unit
) {
    // 1) Instantiate the ViewModel (immediately starts loading):
    val viewModel = remember { PublicProfileViewModel(userId = userId, bearer = "Bearer $token") }

    // 2) Collect all the StateFlow values:
    val name         by viewModel.name.collectAsState()
    val avatarUrl    by viewModel.avatarUrl.collectAsState()
    val location     by viewModel.location.collectAsState()
    val sports       by viewModel.sports.collectAsState()
    val level        by viewModel.level.collectAsState()
    val behaviour    by viewModel.behaviour.collectAsState()
    val repLabel     by viewModel.repLabel.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val errorMsg     by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* no title, just back arrow */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // ─── Avatar ───────────────────────────────
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                if (avatarUrl != null) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ─── Name under avatar ──────────────────────
            Text(
                text = if (name.isNotBlank()) name else "–",
                fontSize = 20.sp,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(12.dp))

            // ─── Location & Favourite Sports ──────────
            Text(
                text = "Location: ${location ?: "–"}",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Favourite Sports: ${if (sports.isNotEmpty()) sports.joinToString(", ") else "–"}",
                style = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            // ─── Stats Card (Level • Behaviour • Reputation) ───────
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStat("Level", level.toString())
                    ProfileStat("Behaviour", behaviour?.toString() ?: "–")
                    ProfileStat("Reputation", if (repLabel.isNotBlank()) repLabel else "–")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Achievements ────────────────────────
            Text(
                text = "Unlocked Achievements",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp)
            )

            if (achievements.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No achievements unlocked",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            } else {
                AchievementsRow(
                    achievements = achievements,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
        }

        Snackbar(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Error: $errorMsg",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                textAlign = TextAlign.Center
            )
        }
    }
}


/* Small stat cell, used above */
@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
