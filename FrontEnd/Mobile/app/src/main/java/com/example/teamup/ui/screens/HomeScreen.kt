// File: app/src/main/java/com/example/teamup/ui/screens/HomeScreen.kt
package com.example.teamup.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.ui.components.ActivityCard
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

// ▶ CORRECT imports for Compose‐Maps:
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.rememberMarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@Composable
fun HomeScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit,
    viewModel: HomeViewModel
) {
    // 1) Observe activities + error + center from the ViewModel
    val activities by viewModel.activities.collectAsState()
    val error      by viewModel.error.collectAsState()
    val center     by viewModel.center.collectAsState()

    // 2) Get a Context for geocoding fallback
    val ctx = LocalContext.current

    // 3) Load the user’s activities whenever `token` changes
    LaunchedEffect(token) {
        viewModel.loadActivities(token)
    }

    // 4) Once at startup (or if token changes), attempt fallback‐center logic
    LaunchedEffect(token) {
        viewModel.loadFallbackCenter(token, ctx)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MapView(
            modifier   = Modifier
                .height(300.dp)
                .fillMaxWidth(),
            activities = activities,
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
            activities = activities,
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
    onActivityClick: (ActivityItem) -> Unit
) {
    Text(
        text = "Available Activities nearby:",
        color = Color(0xFF023499),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )

    if (activities.isEmpty()) {
        Text(
            text = "No activities near you 😞",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(activities, key = { it.id }) { act ->
                ActivityCard(
                    activity = act,
                    bgColor       = if (act.isCreator) Color(0xFFE3F2FD) else Color(0xFFF5F5F5),
                    labelCreator  = if (act.isCreator) "You are the creator" else null,
                    onClick = { onActivityClick(act) }
                )
            }
        }
    }
}
