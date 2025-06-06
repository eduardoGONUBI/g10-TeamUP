// File: app/src/main/java/com/example/teamup/ui/screens/Profile/PublicProfileScreen.kt
package com.example.teamup.ui.screens.Profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.teamup.presentation.profile.PublicProfileViewModel
import com.example.teamup.ui.components.ActivityCard
import com.example.teamup.ui.components.AchievementsRow
import com.example.teamup.data.domain.model.ActivityItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    token: String,
    userId: Int,
    onBack: () -> Unit,
    onEventClick: (ActivityItem) -> Unit
) {
    // 1) Instantiate the ViewModel (immediately starts loading)
    val viewModel = remember { PublicProfileViewModel(userId = userId, bearer = "Bearer $token") }

    // 2) Collect all the StateFlow values
    val name         by viewModel.name.collectAsState()
    val avatarUrl    by viewModel.avatarUrl.collectAsState()
    val location     by viewModel.location.collectAsState()
    val sports       by viewModel.sports.collectAsState()
    val level        by viewModel.level.collectAsState()
    val behaviour    by viewModel.behaviour.collectAsState()
    val repLabel     by viewModel.repLabel.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val errorMsg     by viewModel.error.collectAsState()

    // ─── NEW: collect paginated events + error ───────────────────────────────
    val visibleEvents by viewModel.visibleEvents.collectAsState()
    val hasMoreEvents by viewModel.hasMoreEvents.collectAsState()
    val eventsError   by viewModel.eventsError.collectAsState()

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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ── Spacer overhead ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // ── Avatar ──────────────────────────────────────────────────────────
            item {
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
            }

            // ── Name under avatar ────────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (name.isNotBlank()) name else "–",
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // ── Location & Favourite Sports ─────────────────────────────────────
            item {
                Text(
                    text = "Location: ${location ?: "–"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Favourite Sports: ${if (sports.isNotEmpty()) sports.joinToString(", ") else "–"}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            // ── Stats Card (Level • Behaviour • Reputation) ─────────────────────
            item {
                Spacer(modifier = Modifier.height(16.dp))
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
            }

            // ── Achievements Header ──────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "Unlocked Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }

            // ── Achievements Row or “No achievements” ──────────────────────────
            item {
                if (achievements.isEmpty()) {
                    Text(
                        text = "No achievements unlocked",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                    )
                } else {
                    AchievementsRow(
                        achievements = achievements,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, bottom = 8.dp)
                    )
                }
            }

            // ── Spacer before Events ─────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }

            // ── Events Header ───────────────────────────────────────────────────
            item {
                Text(
                    text = "Events Created",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }

            // ── Show eventsError if non‐null ────────────────────────────────────
            if (eventsError != null) {
                item {
                    Text(
                        text = "Failed to load events: $eventsError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // ── If no visibleEvents AND no error, show “No events created” ─────
            if (visibleEvents.isEmpty() && eventsError == null) {
                item {
                    Text(
                        text = "No events created",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // ── Otherwise, list each event as an ActivityCard ───────────────────
            if (visibleEvents.isNotEmpty()) {
                items(visibleEvents, key = { it.id }) { act ->
                    ActivityCard(
                        activity = act,
                        bgColor = Color(0xFFF5F5F5),
                        onClick = { onEventClick(act) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // ── “Load more” button if there are additional events ────────────────
            if (hasMoreEvents) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Button(
                            onClick = { viewModel.loadMoreEvents() },
                            modifier = Modifier
                                .fillMaxWidth(0.5f)
                                .padding(8.dp)
                        ) {
                            Text("Load more")
                        }
                    }
                }
            }

            // ── Final bottom padding ────────────────────────────────────────────
            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // ─── Global error banner for profile‐related errors ───────────────────
        if (errorMsg != null) {
            Box(modifier = Modifier.fillMaxSize()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text("Error: $errorMsg", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            }
        }
    }
}

/** Reused from ProfileScreen: small stat cell */
@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
