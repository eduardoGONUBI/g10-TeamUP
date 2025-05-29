package com.example.teamup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.data.remote.ActivityItem
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun HomeScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit,
    viewModel: HomeViewModel              // ← Injected ViewModel
) {
    val activities by viewModel.activities.collectAsState()
    val error      by viewModel.error.collectAsState()

    LaunchedEffect(token) {
        viewModel.loadActivities(token)
    }

    Column(Modifier.fillMaxSize()) {
        MapView(
            modifier    = Modifier
                .height(300.dp)
                .fillMaxWidth(),
            activities  = activities
        )

        if (error != null) {
            Text(
                text     = "Failed to load activities: $error",
                color    = Color.Red,
                modifier = Modifier.padding(16.dp)
            )
        }

        ActivitiesList(
            activities      = activities,
            onActivityClick = onActivityClick
        )
    }
}

@Composable
private fun MapView(
    modifier: Modifier = Modifier,
    activities: List<ActivityItem>
) {
    val defaultLocation = LatLng(41.5381, -8.6151)
    val cameraState     = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 10f)
    }

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        GoogleMap(
            modifier             = Modifier.matchParentSize(),
            cameraPositionState  = cameraState
        ) {
            activities.forEach { activity ->
                Marker(
                    state = rememberMarkerState(
                        position = LatLng(activity.latitude, activity.longitude)
                    ),
                    title = activity.title
                )
            }
        }
    }
}

@Composable
fun ActivitiesList(
    activities: List<ActivityItem>,
    onActivityClick: (ActivityItem) -> Unit
) {
    Text(
        text     = "Your Activities:",
        color    = Color(0xFF023499),
        style    = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )

    if (activities.isEmpty()) {
        Text(
            text     = "No activities available",
            color    = Color(0xFF023499),
            style    = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    } else {
        LazyColumn(
            modifier           = Modifier.fillMaxWidth(),
            verticalArrangement= Arrangement.spacedBy(12.dp),
            contentPadding     = PaddingValues(vertical = 12.dp, horizontal = 24.dp)
        ) {
            items(activities, key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick  = { onActivityClick(activity) }
                )
            }
        }
    }
}

@Composable
fun ActivityCard(
    activity: ActivityItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .shadow(2.dp, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            modifier           = Modifier.fillMaxWidth(),
            horizontalArrangement= Arrangement.SpaceBetween,
            verticalAlignment  = Alignment.CenterVertically
        ) {
            Text(
                text  = activity.title,
                color = Color(0xFF023499),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text  = "${activity.participants}/${activity.maxParticipants} Participants",
                color = Color(0xFF023499),
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text  = "Location: ${activity.location}",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text  = "Date: ${activity.date}",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodySmall
        )
        Text(
            text  = "Organizer: ${activity.organizer}",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodySmall
        )

        Spacer(Modifier.height(8.dp))

        Row(
            modifier            = Modifier.fillMaxWidth(),
            horizontalArrangement= Arrangement.End
        ) {
            Text(
                text     = "See Activity",
                color    = Color(0xFF023499),
                style    = MaterialTheme.typography.bodyMedium,
                fontSize = 14.sp
            )
        }
    }
}
