// File: app/src/main/java/com/example/teamup/ui/screens/HomeScreen.kt
package com.example.teamup.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.ui.components.ActivityCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

// ▶ CORRECT imports for Compose‐Maps:
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberCameraPositionState
import com.google.maps.android.compose.rememberMarkerState

@Composable
fun HomeScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit
) {
    // 1) Create / Hoist the VM with a working factory that injects ActivityRepositoryImpl:
    val vm: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Build repo exactly as you do elsewhere:
                val repo: ActivityRepository = ActivityRepositoryImpl(ActivityApi.create())
                return HomeViewModel(repo) as T
            }
        }
    )

    // 2) Observe all needed state:
    //    - “activities” is the full filtered list for the map
    //    - “visibleActivities” is the paged subset for the LazyColumn
    //    - “hasMore” decides whether to show “Load more”
    val allActivities     by vm.activities.collectAsState()
    val visibleActivities by vm.visibleActivities.collectAsState()
    val hasMore           by vm.hasMore.collectAsState()
    val error             by vm.error.collectAsState()
    val center            by vm.center.collectAsState()

    // 3) Get a Context for geocoding
    val ctx = LocalContext.current

    // 4) Trigger both loads when `token` changes:
    LaunchedEffect(token) {
        vm.loadActivities(token)
        vm.loadFallbackCenter(token, ctx)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MapView(
            modifier   = Modifier
                .height(300.dp)
                .fillMaxWidth(),
            activities = allActivities,
            center     = center
        )

        error?.let {
            Text(
                text = "Failed to load activities: $it",
                color = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }

        ActivitiesList(
            activities      = visibleActivities,
            hasMore         = hasMore,
            onLoadMore      = { vm.loadMore() },
            onActivityClick = onActivityClick
        )
    }
}

@Composable
private fun MapView(
    modifier: Modifier,
    activities: List<ActivityItem>,
    center: LatLng
) {
    // Use `center` as the initial camera position. Zoom = 11f by default.
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 11f)
    }

    // Animate camera whenever `center` changes
    LaunchedEffect(center) {
        cameraState.animate(CameraUpdateFactory.newLatLngZoom(center, 11f))
    }

    Box(modifier = modifier.background(Color.White, MaterialTheme.shapes.medium)) {
        GoogleMap(
            modifier = Modifier.matchParentSize(),
            cameraPositionState = cameraState
        ) {
            activities.forEach { act ->
                Marker(
                    state = rememberMarkerState(position = LatLng(act.latitude, act.longitude)),
                    title = act.title
                )
            }
        }
    }
}

@Composable
private fun ActivitiesList(
    activities: List<ActivityItem>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onActivityClick: (ActivityItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        item {
            Text(
                text = "Available Activities nearby:",
                color = Color(0xFF023499),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }

        if (activities.isEmpty()) {
            // No activities at all (after filtering). Show placeholder.
            item {
                Text(
                    text = "No activities near you 😞",
                    color = Color(0xFF023499),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
        } else {
            // Show only the paged subset
            items(activities, key = { it.id }) { act ->
                ActivityCard(
                    activity      = act,
                    bgColor       = if (act.isCreator) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                    labelCreator  = if (act.isCreator) "You are the creator" else null,
                    onClick       = { onActivityClick(act) }
                )
            }

            // “Load more” button if there’s another page
            if (hasMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        Button(
                            onClick = onLoadMore,
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
