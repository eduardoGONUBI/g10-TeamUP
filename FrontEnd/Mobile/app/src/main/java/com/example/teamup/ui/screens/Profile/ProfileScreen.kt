// File: app/src/main/java/com/example/teamup/ui/screens/ProfileScreen.kt
package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.presentation.profile.ProfileViewModel
import com.example.teamup.ui.components.ActivityCard
import com.example.teamup.ui.components.AchievementsRow
import com.example.teamup.R

@Composable
fun ProfileScreen(
    token: String,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onActivityClick: (ActivityItem) -> Unit,
    viewModel: ProfileViewModel
) {
    // ─── Collect all state from ViewModel ──────────────────────────────
    val username         by viewModel.username.collectAsState()
    val level            by viewModel.level.collectAsState()
    val behaviour        by viewModel.behaviour.collectAsState()
    val reputationLabel  by viewModel.reputationLabel.collectAsState()
    val achievements     by viewModel.achievements.collectAsState()
    val activities       by viewModel.createdActivities.collectAsState()
    val error            by viewModel.error.collectAsState()
    val activitiesError  by viewModel.activitiesError.collectAsState()

    // ─── Trigger loads whenever token changes ───────────────────────────
    LaunchedEffect(token) {
        viewModel.loadUser(token)
        viewModel.loadCreatedActivities(token)
        viewModel.loadStats(token)
    }

    // ─── Wrap both the scrollable content and the Snackbar in a Box ───
    Box(modifier = Modifier.fillMaxSize()) {
        // ────────────────────────────
        // 1) Main scrolling content
        // ────────────────────────────
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Avatar + Edit Icon ───────────────────────────────────
            item {
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.avatar_default),
                        contentDescription = "Default Avatar",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .align(Alignment.Center),
                        contentScale = ContentScale.Crop
                    )
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit photo",
                        modifier = Modifier
                            .offset((-4).dp, (-4).dp)
                            .size(24.dp)
                            .clickable(onClick = onEditProfile)
                    )
                }
            }

            // ── Username ─────────────────────────────────
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = username,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // ── Stats Card ───────────────────────────────────────────
            item {
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
                        ProfileStat("Behaviour", behaviour?.toInt()?.toString() ?: "—")
                        ProfileStat("Reputation", reputationLabel)
                    }
                }
            }

            // ── Achievements Header ───────────────────────────────────
            item {
                Text(
                    text = "Unlocked Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // ── Achievements Icons Row ───────────────────────────────
            item {
                AchievementsRow(achievements)
            }

            // ── Edit & Logout Buttons ─────────────────────────────────
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp)
                ) {
                    Button(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Profile")
                    }
                    OutlinedButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }

            // ── Recent Activities Header ──────────────────────────────
            item {
                Text(
                    text = "Recent Activities Created",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // ── Recent Activities List or Error ──────────────────────
            when {
                activitiesError != null -> item {
                    Text(
                        "Failed to load activities: $activitiesError",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp)
                    )
                }
                activities.isEmpty() -> item {
                    Text(
                        "No activities created",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
                else -> items(activities, key = { it.id }) { act ->
                    ActivityCard(
                        activity = act,
                        onClick = { onActivityClick(act) }
                    )
                }
            }
        }

        // ────────────────────────────
        // 2) Error banner (generic)
        // ────────────────────────────
        if (error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Error: $error")
            }
        }
    }
}

/* Small stat cell */
@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, style = MaterialTheme.typography.titleMedium)
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
