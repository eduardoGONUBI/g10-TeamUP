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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.R
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.presentation.profile.ProfileViewModel
import com.example.teamup.ui.components.ActivityCard

@Composable
fun ProfileScreen(
    token: String,
    onEditProfile: () -> Unit,
    onLogout: () -> Unit,
    onActivityClick: (ActivityItem) -> Unit,
    viewModel: ProfileViewModel
) {
    /* ── state ─────────────────────────────────────────────── */
    val username         by viewModel.username.collectAsState()
    val userError        by viewModel.error.collectAsState()
    val activities       by viewModel.createdActivities.collectAsState()
    val activitiesError  by viewModel.activitiesError.collectAsState()

    /* ── load data on token change ─────────────────────────── */
    LaunchedEffect(token) {
        viewModel.loadUser(token)
        viewModel.loadCreatedActivities(token)
    }

    /* ── UI ────────────────────────────────────────────────── */
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        /* Header image */
        item {
            Image(
                painter          = painterResource(R.drawable.icon_up),
                contentDescription = "Header",
                modifier         = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentScale     = ContentScale.Crop
            )
        }

        /* Avatar + edit overlay */
        item {
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(y = (-40).dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.icon_up),
                    contentDescription = "Profile photo",
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                )
                Icon(
                    imageVector  = Icons.Default.Edit,
                    contentDescription = "Change photo",
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(4.dp, 4.dp)
                        .size(24.dp)
                        .clickable(onClick = onEditProfile)
                )
            }
        }

        /* Username */
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 16.dp)
            ) {
                Text(text = username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(
                    text  = "Location",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        /* Profile fetch error */
        if (userError != null) {
            item {
                Text(
                    text     = "Failed to load profile: $userError",
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        /* Stats */
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                ProfileStat("Organized",    "${activities.count { it.isCreator }}")
                ProfileStat("Participated", "0")
                ProfileStat("Rating",       "0.0")
            }
        }

        /* Edit / logout buttons */
        item {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(onClick = onEditProfile) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Edit Profile")
                }
                OutlinedButton(onClick = onLogout) {
                    Icon(Icons.Default.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Logout")
                }
            }
        }

        /* Activities header */
        item {
            Text(
                text = "Recent Activities Created",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
            )
        }

        /* Activities list / error handling */
        when {
            activitiesError != null -> item {
                Text(
                    text  = "Failed to load activities: $activitiesError",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
                )
            }

            activities.isEmpty() -> item {
                Text(
                    text  = "No activities created",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                )
            }

            else -> items(activities, key = { it.id }) { activity ->
                /* Use the shared ActivityCard from HomeScreen */
                ActivityCard(activity = activity) {
                    onActivityClick(activity)
                }
            }
        }
    }
}

/* ── helper composable for stats row ───────────────────────── */
@Composable
private fun ProfileStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(text = label, fontSize = 12.sp)
    }
}
