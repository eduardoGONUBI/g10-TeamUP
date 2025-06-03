// app/src/main/java/com/example/teamup/ui/screens/activityManager/YourActivitiesScreen.kt
package com.example.teamup.ui.screens.activityManager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
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

/**
 * Shows all activities the user created or joined. Uses the shared [ActivityCard]
 * for consistent styling and sport‐icon display across the app.
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
                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity) }
                )
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
