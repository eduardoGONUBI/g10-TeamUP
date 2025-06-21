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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.teamup.R
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.BaseUrlProvider
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.presentation.profile.ProfileViewModel
import com.example.teamup.ui.components.ActivityCard
import com.example.teamup.ui.components.AchievementsRow
import com.example.teamup.ui.popups.LogoutDialog

@Composable
fun ProfileScreen(
    token: String,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onActivityClick: (ActivityItem) -> Unit
) {
    // Hoist ViewModel with injected repository
    val viewModel: ProfileViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo: ActivityRepository = ActivityRepositoryImpl(ActivityApi.create())
                return ProfileViewModel(repo) as T
            }
        }
    )

    // Collect state
    val username        by viewModel.username.collectAsState()
    val location        by viewModel.location.collectAsState()
    val sports          by viewModel.sports.collectAsState()
    val level           by viewModel.level.collectAsState()
    val behaviour       by viewModel.behaviour.collectAsState()
    val reputationLabel by viewModel.reputationLabel.collectAsState()
    val achievements    by viewModel.achievements.collectAsState()
    val activities      by viewModel.visibleCreatedActivities.collectAsState()
    val hasMore         by viewModel.hasMoreCreated.collectAsState()
    val error           by viewModel.error.collectAsState()
    val activitiesError by viewModel.activitiesError.collectAsState()
    val avatarUrl by viewModel.avatarUrl.collectAsState()

    var showLogoutDialog by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(Unit) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadUser(token)            // refaz pedido /me
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // Trigger loads
    LaunchedEffect(token) {
        viewModel.loadUser(token)
        viewModel.loadCreatedActivities(token)
        viewModel.loadStats(token)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

            // Avatar
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    val fullUrl = when {
                        avatarUrl.isNullOrBlank()            -> null                     // usa fallback
                        avatarUrl!!.startsWith("http")       -> avatarUrl                // URL absoluta
                        else -> {                                                      // caminho relativo
                            BaseUrlProvider.getBaseUrl().trimEnd('/') +
                                    "/" + avatarUrl!!.trimStart('/')
                        }
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(fullUrl ?: R.drawable.fotografia)   // fallback se for null
                            .crossfade(true)
                            .build(),
                        contentDescription = "Avatar",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }


            // Username
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = username,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            // Location & Favourite Sports
            item {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Text(
                        text = "Location: ${location ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Favourite Sports: ${
                            if (sports.isNotEmpty()) sports.joinToString(", ") else "—"
                        }",
                        style = MaterialTheme.typography.bodyMedium,
                        fontSize = 14.sp
                    )
                }
            }

            // Stats Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                    elevation = androidx.compose.material3.CardDefaults.cardElevation(4.dp)
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

            // Achievements Header
            item {
                Text(
                    text = "Unlocked Achievements",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // Achievements Row
            item {
                AchievementsRow(achievements)
            }

            // Edit & Logout Buttons
            item {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                ) {
                    Button(onClick = onEditProfile) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        Spacer(Modifier.width(8.dp))
                        Text("Edit Profile")
                    }
                    OutlinedButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                        Spacer(Modifier.width(8.dp))
                        Text("Logout")
                    }
                }
            }

            // Recent Activities Header
            item {
                Text(
                    text = "Recent Activities Created",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 24.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            // Recent Activities List / Error / Load More
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
                else -> {
                    items(activities, key = { it.id }) { act ->
                        ActivityCard(
                            activity = act,
                            bgColor = Color(0xFFF5F5F5),
                            onClick = { onActivityClick(act) }
                        )
                    }
                    if (hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { viewModel.loadMoreCreated() },
                                    modifier = Modifier
                                        .fillMaxWidth(0.5f)
                                        .padding(8.dp)
                                ) {
                                    Text("Load more")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Generic error banner
        if (error != null) {
            Snackbar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Text("Error: $error")
            }
        }

        // Logout confirmation dialog
        if (showLogoutDialog) {
            Dialog(onDismissRequest = { showLogoutDialog = false }) {
                LogoutDialog(
                    onCancel = { showLogoutDialog = false },
                    onLogout = {
                        showLogoutDialog = false
                        onLogout()
                    }
                )
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
