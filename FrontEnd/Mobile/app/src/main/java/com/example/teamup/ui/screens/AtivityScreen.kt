package com.example.teamup.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.Indicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.teamup.R
import kotlinx.coroutines.launch

/** Re-use your Activity model and ActivityCard from HomeScreen file **/
import com.example.teamup.ui.screens.Activity
import com.example.teamup.ui.screens.ActivityCard

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AtivityScreen(
    modifier: Modifier = Modifier,
    yourActivities: List<Activity> = emptyList(),
    onActivityClick: (Activity) -> Unit = {}
) {
    val tabs = listOf("Search Activity", "Create Activity", "Your Activities")
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val scope = rememberCoroutineScope()
    val blue = Color(0xFF023499)

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.White,
            contentColor = blue,
            indicator = { tabPositions ->
                Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                    color = blue
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    text = {
                        Text(
                            title,
                            color = if (pagerState.currentPage == index) blue else Color.Gray
                        )
                    },
                    selected = pagerState.currentPage == index,
                    onClick = { scope.launch { pagerState.animateScrollToPage(index) } }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> SearchTabContent(
                    activities = yourActivities,
                    onActivityClick = onActivityClick
                )
                1 -> CreateActivityTab(blue = blue)
                2 -> YourActivitiesTab(
                    activities = yourActivities,
                    onActivityClick = onActivityClick
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTabContent(
    activities: List<Activity>,
    onActivityClick: (Activity) -> Unit
) {
    val blue = Color(0xFF023499)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        FiltersSection(blue = blue)
        Spacer(Modifier.height(24.dp))

        Text(
            text = "Available Activities:",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            color = blue
        )
        Spacer(Modifier.height(12.dp))

        if (activities.isEmpty()) {
            Text(
                text = "No activities available",
                color = blue,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateActivityTab(
    blue: Color = Color(0xFF023499)
) {
    var name by remember { mutableStateOf("") }
    var sportExpanded by remember { mutableStateOf(false) }
    var selectedSport by remember { mutableStateOf<String?>(null) }
    val sports = listOf("Football", "Basketball", "Tennis", "Badminton", "Hockey")
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var participants by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Create Activity",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            color = blue
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Activity Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = sportExpanded,
            onExpandedChange = { sportExpanded = !sportExpanded }
        ) {
            OutlinedTextField(
                value = selectedSport ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Sport") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = sportExpanded,
                onDismissRequest = { sportExpanded = false }
            ) {
                sports.forEach { sport ->
                    DropdownMenuItem(
                        text = { Text(sport) },
                        onClick = {
                            selectedSport = sport
                            sportExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Location") },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_sports_soccer_24),
                    contentDescription = "Location"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Date") },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.atividades),
                    contentDescription = "Date"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = time,
            onValueChange = { time = it },
            label = { Text("Time") },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_sports_soccer_24),
                    contentDescription = "Time"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = participants,
            onValueChange = { participants = it },
            label = { Text("Number of Participants") },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_person_24),
                    contentDescription = "Participants"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = { /* TODO: call create-activity API */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = blue,
                contentColor = Color.White
            )
        ) {
            Text("Create Activity")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YourActivitiesTab(
    activities: List<Activity>,
    onActivityClick: (Activity) -> Unit
) {
    val blue = Color(0xFF023499)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Your Activities:",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            color = blue
        )
        Spacer(Modifier.height(12.dp))

        if (activities.isEmpty()) {
            Text(
                text = "No activities yet",
                color = blue,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FiltersSection(blue: Color) {
    var name by remember { mutableStateOf("") }
    var sportExpanded by remember { mutableStateOf(false) }
    var selectedSport by remember { mutableStateOf<String?>(null) }
    val sports = listOf("Football", "Basketball", "Tennis", "Badminton", "Hockey")
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    Column {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium,
            fontSize = 18.sp,
            color = blue
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Search by Name") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        ExposedDropdownMenuBox(
            expanded = sportExpanded,
            onExpandedChange = { sportExpanded = !sportExpanded }
        ) {
            OutlinedTextField(
                value = selectedSport ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Search by Sport") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = sportExpanded)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = sportExpanded,
                onDismissRequest = { sportExpanded = false }
            ) {
                sports.forEach { sport ->
                    DropdownMenuItem(
                        text = { Text(sport) },
                        onClick = {
                            selectedSport = sport
                            sportExpanded = false
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = location,
            onValueChange = { location = it },
            label = { Text("Search by Location") },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_sports_soccer_24),
                    contentDescription = "Location"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Search by Date") },
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.date1),
                    contentDescription = "Date"
                )
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
