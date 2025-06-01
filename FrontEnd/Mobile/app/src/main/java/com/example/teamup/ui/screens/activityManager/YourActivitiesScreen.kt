// app/src/main/java/com/example/teamup/ui/screens/activityManager/YourActivitiesScreen.kt
package com.example.teamup.ui.screens.activityManager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.data.remote.ActivityRepositoryImpl

/**
 * “Your Activities” screen.
 *
 * This composable:
 *  1. Instantiates a YourActivitiesViewModel (which calls repo.getMyActivities(token))
 *  2. Shows any error at the top
 *  3. Displays a LazyColumn of all events you’ve created
 *  4. Colors each card a light‐blue if `activity.isCreator == true`
 */
@Composable
fun YourActivitiesScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit
) {
    // 1) Build repository & ViewModel
    val repo = remember { ActivityRepositoryImpl(ActivityApi.create()) }

    val yourVm: YourActivitiesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return YourActivitiesViewModel(repo) as T
            }
        }
    )

    // 2) Observe state
    val activities by yourVm.activities.collectAsState()
    val errorText by yourVm.error.collectAsState()

    // 3) Trigger load when first composed or when token changes
    LaunchedEffect(token) {
        yourVm.loadMyActivities(token)
    }

    // 4) UI
    Column(modifier = Modifier.fillMaxSize()) {
        // 4a) Show error (if any)
        if (!errorText.isNullOrEmpty()) {
            Text(
                text = errorText ?: "",
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
        }

        // 4b) Show “no activities” message if empty
        if (activities.isEmpty() && errorText.isNullOrEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("You haven’t created any activities yet.")
            }
        }

        // 4c) Otherwise, list them in a LazyColumn
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activities, key = { it.id }) { activity ->
                // If this item.wasCreatedByMe, use a light‐blue background; else white.
                // Since getMyActivities returns only created events, isCreator is always true here.
                val bgColor = if (activity.isCreator) Color(0xFFE3F2FD) else Color.White

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(2.dp, RoundedCornerShape(8.dp))
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .clickable { onActivityClick(activity) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = activity.title,
                            color = Color(0xFF023499),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "${activity.participants}/${activity.maxParticipants} Participants",
                            color = Color(0xFF023499),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Location: ${activity.location}",
                        color = Color(0xFF023499),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Date: ${activity.date}",
                        color = Color(0xFF023499),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Organizer: ${activity.organizer}",
                        color = Color(0xFF023499),
                        style = MaterialTheme.typography.bodySmall
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "See Activity",
                            color = Color(0xFF023499),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}
