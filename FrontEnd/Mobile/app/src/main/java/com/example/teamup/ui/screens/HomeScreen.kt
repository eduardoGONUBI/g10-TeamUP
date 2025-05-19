package com.example.teamup.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

/** Your activity data model **/
data class Activity(
    val id: String,
    val title: String,
    val location: String,
    val date: String,
    val participants: Int,
    val maxParticipants: Int
)

@Composable
fun HomeScreen(
    activities: List<Activity>,
    onActivityClick: (Activity) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        MapView(
            modifier = Modifier
                .height(300.dp)
                .fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        ActivitiesList(
            activities = activities,
            onActivityClick = onActivityClick
        )
    }
}

@Composable
private fun MapView(modifier: Modifier = Modifier) {
    val barcelos = LatLng(41.5381, -8.6151)
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(barcelos, 13f)
    }

    Box(
        modifier = modifier
            .shadow(4.dp, RoundedCornerShape(8.dp))
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White, RoundedCornerShape(8.dp))
    ) {
        GoogleMap(
            modifier            = Modifier.matchParentSize(),
            cameraPositionState = cameraState
        ) {
            // <-- fully named parameters here:
            Marker(
                state = rememberMarkerState(position = barcelos),
                title = "Barcelos"
            )
        }
    }
}

@Composable
fun ActivitiesList(
    activities: List<Activity>,
    onActivityClick: (Activity) -> Unit
) {
    // header always shows
    Text(
        text = "Available Activities:",
        color = Color(0xFF023499),
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    )

    if (activities.isEmpty()) {
        Text(
            text = "No activities available",
            color = Color(0xFF023499),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp, horizontal = 24.dp)
        ) {
            items(activities, key = { it.id }) { activity ->
                ActivityCard(
                    activity = activity,
                    onClick = { onActivityClick(activity) }
                )
            }
        }
    }
}

@Composable
public fun ActivityCard(
    activity: Activity,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text  = activity.title,
                color = Color(0xFF023499),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text  = "${activity.participants}/${activity.maxParticipants}",
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

        Spacer(Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
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
