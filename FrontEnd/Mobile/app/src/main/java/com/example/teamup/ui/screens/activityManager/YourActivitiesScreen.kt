// File: app/src/main/java/com/example/teamup/ui/screens/activityManager/YourActivitiesScreen.kt
package com.example.teamup.ui.screens.activityManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.ui.components.ActivityCard

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

    // 5) UI: reuse ActivityCard for each item
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // If there are any activities, display them
        if (uiState.activities.isNotEmpty()) {
            items(uiState.activities, key = { it.id }) { activity ->
                // Decide background (flat color or gradient) and labels:
                val bgColor: Color
                var bgBrush: Brush? = null
                var labelCreator: String? = null
                var labelConcluded: String? = null

                when {
                    // 1) Creator & concluded → gradient from pale blue → orange
                    activity.isCreator && activity.status == "concluded" -> {
                        bgBrush = Brush.horizontalGradient(
                            listOf(
                                Color(0xFFE3F2FD), // very pale blue
                                Color(0xFFFFA500)  // orange
                            )
                        )
                        bgColor = Color.Transparent
                        labelCreator = "You are the creator"
                        labelConcluded = "Event concluded"
                    }

                    // 2) Only creator (and not concluded) → pale blue
                    activity.isCreator -> {
                        bgColor = Color(0xFFE3F2FD) // #E3F2FD
                        labelCreator = "You are the creator"
                        labelConcluded = null
                    }

                    // 3) Only concluded (and not creator) → solid orange
                    activity.status == "concluded" -> {
                        bgColor = Color(0xFFFFA500)
                        labelCreator = null
                        labelConcluded = "Event concluded"
                    }

                    // 4) Only participant (not creator, not concluded) → also pale blue
                    activity.isParticipant -> {
                        bgColor = Color(0xFFF5F5F5)
                        labelCreator = null
                        labelConcluded = null
                    }

                    // 5) Neither → default lavender
                    else -> {
                        bgColor = Color(0xFF3629B7)
                        labelCreator = null
                        labelConcluded = null
                    }
                }

                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity) },
                    bgColor = bgColor,
                    bgBrush = bgBrush,
                    labelCreator = labelCreator,
                    labelConcluded = labelConcluded
                )
            }
        }
        // If no activities at all, show placeholder
        else if (uiState.error == null) {
            item {
                Text(
                    text = "You have no activities.",
                    color = Color.Gray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }
        }

        // If there was an error when loading
        uiState.error?.let { err ->
            item {
                Text(
                    text = "Failed to load your activities: $err",
                    color = Color.Red,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
            }
        }
    }
}
