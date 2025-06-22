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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.teamup.domain.model.Activity
import com.example.teamup.domain.repository.ActivityRepository
import com.example.teamup.data.remote.repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.presentation.profile.PublicProfileViewModel
import com.example.teamup.ui.components.ActivityCard
import com.example.teamup.ui.components.AchievementsRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    token: String,
    userId: Int,
    onBack: () -> Unit,
    onEventClick: (Activity) -> Unit
) {
    // 1) Corona o ViewModel via factory (injetando o repo e o bearer)
    val viewModel: PublicProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo: ActivityRepository = ActivityRepositoryImpl(ActivityApi.create())
                return PublicProfileViewModel(
                    userId = userId,
                    bearer = "Bearer $token"
                ) as T
            }
        }
    )

    // 2) Collect all StateFlows
    val name         by viewModel.name.collectAsState()
    val avatarUrl    by viewModel.avatarUrl.collectAsState()
    val location     by viewModel.location.collectAsState()
    val sports       by viewModel.sports.collectAsState()
    val level        by viewModel.level.collectAsState()
    val behaviour    by viewModel.behaviour.collectAsState()
    val repLabel     by viewModel.repLabel.collectAsState()
    val achievements by viewModel.achievements.collectAsState()
    val errorMsg     by viewModel.error.collectAsState()

    // ─── Paginated events ─────────────────────────────────────────────
    val visibleEvents by viewModel.visibleEvents.collectAsState()
    val hasMoreEvents by viewModel.hasMoreEvents.collectAsState()
    val eventsError   by viewModel.eventsError.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* no title */ },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            // Spacer top
            item { Spacer(Modifier.height(16.dp)) }

            // Avatar
            item {
                Box(
                    Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
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
                            Icons.Default.Person,
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

            // Name
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = name.ifBlank { "–" },
                    fontSize = 20.sp,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Location & Sports
            item {
                Column(Modifier.padding(horizontal = 24.dp)) {
                    Text(
                        text = "Location: ${location ?: "–"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Favourite Sports: ${
                            if (sports.isNotEmpty()) sports.joinToString(", ") else "–"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                }
            }

            // Stats Card
            item {
                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ProfileStat("Level", level.toString())
                        ProfileStat("Behaviour", behaviour?.toString() ?: "–")
                        ProfileStat("Reputation", repLabel.ifBlank { "–" })
                    }
                }
            }

            // Achievements
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "Unlocked Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }
            item {
                if (achievements.isEmpty()) {
                    Text(
                        "No achievements unlocked",
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

            // Spacer
            item { Spacer(Modifier.height(24.dp)) }

            // Events Header
            item {
                Text(
                    text = "Events Created",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
                )
            }

            // Events Error
            if (eventsError != null) {
                item {
                    Text(
                        "Failed to load events: $eventsError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // No events placeholder
            if (visibleEvents.isEmpty() && eventsError == null) {
                item {
                    Text(
                        "No events created",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }

            // Event items
            if (visibleEvents.isNotEmpty()) {
                items(visibleEvents, key = { it.id }) { act ->
                    ActivityCard(
                        activity = act,
                        bgColor = Color(0xFFF5F5F5),
                        onClick = { onEventClick(act) }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Load more
            if (hasMoreEvents) {
                item {
                    Box(
                        Modifier
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

            // Bottom padding
            item { Spacer(Modifier.height(32.dp)) }
        }

        // Global error banner
        if (errorMsg != null) {
            Box(Modifier.fillMaxSize()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                ) {
                    Text(
                        "Error: $errorMsg",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
