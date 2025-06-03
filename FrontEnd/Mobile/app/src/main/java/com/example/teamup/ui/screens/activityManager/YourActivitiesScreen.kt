package com.example.teamup.ui.screens.activityManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl

/**
 * Shows all activities the user created or joined. If the user is the creator of an activity,
 * the card’s background is tinted light‐blue.  If an activity is concluded, it’s tinted orange.
 * If it’s both concluded AND I’m the creator, it uses a blue↔orange gradient.
 */
@Composable
fun YourActivitiesScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit
) {
    // 1) Build repository
    val repo: ActivityRepository = remember {
        ActivityRepositoryImpl(ActivityApi.create())
    }

    // 2) Hoist ViewModel
    val vm: YourActivitiesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return YourActivitiesViewModel(repo) as T
            }
        }
    )

    // 3) Observe state
    val uiState by vm.state.collectAsState()

    // 4) Trigger load
    LaunchedEffect(token) {
        vm.loadMyEvents(token)
    }

    // 5) UI: list or error message
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // If there are any activities, display them
        if (uiState.activities.isNotEmpty()) {
            items(uiState.activities, key = { it.id }) { activity ->
                // Determine if this event is concluded
                val isConcluded = (activity.status != "in progress")

                // Build a Brush (gradient or solid) depending on state:
                val backgroundBrush = when {
                    // both creator AND concluded → gradient (blue ↔ orange)
                    isConcluded && activity.isCreator -> {
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE3F2FD),        // same light‐blue used before
                                Color(0xFFFFA726)         // orange
                            )
                        )
                    }
                    // only concluded → solid orange
                    isConcluded -> {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFFFA726),
                                Color(0xFFFFA726)
                            )
                        )
                    }
                    // only creator (and not concluded) → solid light‐blue (as before)
                    activity.isCreator -> {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE3F2FD),
                                Color(0xFFE3F2FD)
                            )
                        )
                    }
                    // neither creator nor concluded → white
                    else -> {
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White,
                                Color.White
                            )
                        )
                    }
                }

                // Wrap a transparent Card with a Box that uses our brush
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onActivityClick(activity) },
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Box(
                        modifier = Modifier
                            .background(brush = backgroundBrush, shape = MaterialTheme.shapes.medium)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            // Title + chevron
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = activity.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color(0xFF023499)
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = "Go to details",
                                    tint = Color(0xFF023499)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Date: ${activity.date}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF023499)
                            )
                            Text(
                                text = "Location: ${activity.location}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF023499)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "${activity.participants}/${activity.maxParticipants} Participants",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF023499)
                            )

                            // Only show this badge if the user is the creator
                            ActivityCreatorBadge(isCreator = activity.isCreator)
                        }
                    }
                }
            }
        }
        // If no activities at all, show "no activities" placeholder
        else if (uiState.error == null) {
            item {
                Text(
                    text = "You have no activities.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }

        // If there was an error when loading
        if (uiState.error != null) {
            item {
                Text(
                    text = "Failed to load your activities: ${uiState.error}",
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}

/**
 * A simple badge that only renders when [isCreator] == true.
 */
@Composable
private fun ActivityCreatorBadge(isCreator: Boolean) {
    if (isCreator) {
        Text(
            text = "You are the creator",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
