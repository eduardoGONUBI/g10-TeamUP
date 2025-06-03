// File: app/src/main/java/com/example/teamup/ui/screens/PublicProfileScreen.kt
package com.example.teamup.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.R
import com.example.teamup.presentation.profile.PublicProfileViewModel
import com.example.teamup.ui.components.AchievementsRow

/**
 * A *read‐only* version of the Profile screen for viewing *other* users.
 * No “Edit” or “Logout” buttons.
 * Just shows avatar, name, stats (level / behaviour / top rep), and unlocked achievements.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PublicProfileScreen(
    token: String,
    userId: Int,
    onBack: () -> Unit
) {
    // 1) Instantiate ViewModel (already fetches in init)
    val viewModel: PublicProfileViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PublicProfileViewModel(userId, "Bearer $token") as T
            }
        }
    )

    // 2) Collect state
    val name         by viewModel.name.collectAsState()
    val level        by viewModel.level.collectAsState()
    val behaviour    by viewModel.behaviour.collectAsState()
    val repLabel     by viewModel.repLabel.collectAsState()
    val ach          by viewModel.achievements.collectAsState()
    val error        by viewModel.error.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Spacer(Modifier.height(24.dp))

                // ─── Avatar (placeholder) ───────────────────────────
                Image(
                    painter = painterResource(id = R.drawable.avatar_default),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(Modifier.height(24.dp))

                // ─── Stats card (Level / Behaviour / Rep) ──────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCell("Level", level.toString())
                        StatCell("Behaviour", behaviour?.toString() ?: "—")
                        StatCell("Reputation", repLabel)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // ─── Unlocked Achievements row ─────────────────────
                Text(
                    text = "Unlocked Achievements",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))
                AchievementsRow(ach, Modifier.padding(horizontal = 16.dp))
            }

            // ─── Error Snackbar at bottom ───────────────────────
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
}

@Composable
private fun StatCell(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 18.sp,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
