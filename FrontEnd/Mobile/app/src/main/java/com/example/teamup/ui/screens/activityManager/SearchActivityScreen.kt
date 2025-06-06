// File: app/src/main/java/com/example/teamup/ui/screens/activityManager/SearchActivityScreen.kt
package com.example.teamup.ui.screens.activityManager

import android.app.DatePickerDialog
import android.annotation.SuppressLint
import android.widget.DatePicker
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
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
import com.example.teamup.data.remote.model.SportDto
import com.example.teamup.ui.components.ActivityCard
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import java.time.LocalDate
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SearchActivityScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit
) {
    val context = LocalContext.current

    // 1) Initialize Google Places (once)
    LaunchedEffect(Unit) {
        if (!Places.isInitialized()) {
            Places.initialize(context, "YOUR_GOOGLE_MAPS_API_KEY")
        }
    }

    // 2) Build repository
    val repo: ActivityRepository = remember {
        ActivityRepositoryImpl(ActivityApi.create())
    }

    // 3) Create ViewModel internally
    val vm: SearchActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchActivityViewModel(repo) as T
            }
        }
    )

    // 4) Observe state
    val state by vm.state.collectAsState()

    // 5) Local UI state for sports dropdown
    var sportsList by remember { mutableStateOf<List<SportDto>>(emptyList()) }
    var sportExpanded by remember { mutableStateOf(false) }

    // 6) Local UI state for Google Places autocomplete
    val placesClient: PlacesClient = remember { Places.createClient(context) }
    val sessionToken = remember { AutocompleteSessionToken.newInstance() }
    var locationSuggestions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }
    var locationDropdownVisible by remember { mutableStateOf(false) }

    // 7) Local UI state for showing Android DatePickerDialog
    var showDatePicker by remember { mutableStateOf(false) }

    // 8) Whether the filter‐panel is expanded or collapsed
    var filtersExpanded by remember { mutableStateOf(true) }

    // 9) Load sports from backend once
    LaunchedEffect(Unit) {
        try {
            val fetched: List<SportDto> = repo.getSports(token)
            sportsList = fetched
        } catch (_: Exception) {
            // ignore or handle error
        }
    }

    // 10) Build an instance of DatePickerDialog when needed
    if (showDatePicker) {
        val todayCalendar = Calendar.getInstance()
        val year = todayCalendar.get(Calendar.YEAR)
        val month = todayCalendar.get(Calendar.MONTH)
        val day = todayCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _: DatePicker, pickedYear: Int, pickedMonth: Int, pickedDay: Int ->
                val pickedDate = LocalDate.of(pickedYear, pickedMonth + 1, pickedDay)
                vm.updateFilter("date", pickedDate.toString())
                showDatePicker = false
            },
            year, month, day
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // Main Column: collapse/expand filter‐panel at top, results fill the rest
    // ─────────────────────────────────────────────────────────────────────────────
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ────────────────
        // Collapsible Filter Panel
        // ────────────────
        Card(
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE3F2FD) // very pale blue  // or use primaryContainer, etc.
            ),
            modifier = Modifier
                .fillMaxWidth()
                // animate height changes when expanded/collapsed
                .animateContentSize()
        ) {
            Column {
                // Header row: “Filters” + toggle arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { filtersExpanded = !filtersExpanded }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = if (filtersExpanded) "Collapse filters" else "Expand filters",
                        modifier = Modifier.rotate(if (filtersExpanded) 180f else 0f)
                    )
                }

                if (filtersExpanded) {
                    // Scrollable container for all filter fields:
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // ————————————————————————————
                        // 1) SEARCH BY NAME
                        // ————————————————————————————
                        OutlinedTextField(
                            value = state.name,
                            onValueChange = { vm.updateFilter("name", it) },
                            label = { Text("Search by Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // ————————————————————————————
                        // 2) SEARCH BY SPORT (dropdown)
                        // ————————————————————————————
                        ExposedDropdownMenuBox(
                            expanded = sportExpanded,
                            onExpandedChange = { sportExpanded = !sportExpanded }
                        ) {
                            OutlinedTextField(
                                value = state.sport,
                                onValueChange = { /* read-only */ },
                                label = { Text("Search by Sport") },
                                readOnly = true,
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(sportExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = sportExpanded,
                                onDismissRequest = { sportExpanded = false }
                            ) {
                                sportsList.forEach { sportDto ->
                                    DropdownMenuItem(
                                        text = { Text(sportDto.name) },
                                        onClick = {
                                            vm.updateFilter("sport", sportDto.name)
                                            sportExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ——————————————————————————————
                        // 3) SEARCH BY LOCATION (autocomplete)
                        // ——————————————————————————————
                        Column {
                            OutlinedTextField(
                                value = state.place,
                                onValueChange = {
                                    vm.updateFilter("place", it)
                                    locationDropdownVisible = true
                                },
                                label = { Text("Search by Location") },
                                singleLine = true,
                                trailingIcon = {
                                    Icon(Icons.Default.Place, contentDescription = null)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )

                            if (locationDropdownVisible && locationSuggestions.isNotEmpty()) {
                                Card(
                                    elevation = CardDefaults.cardElevation(4.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .wrapContentHeight()
                                ) {
                                    LazyColumn {
                                        items(locationSuggestions) { prediction ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(prediction.getFullText(null).toString())
                                                },
                                                onClick = {
                                                    val selected = prediction.getFullText(null).toString()
                                                    vm.updateFilter("place", selected)
                                                    locationDropdownVisible = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // ——————————————————————————————
                        // 4) SEARCH BY DATE (native DatePickerDialog)
                        // ——————————————————————————————
                        OutlinedTextField(
                            value = state.date,
                            onValueChange = { /* read-only */ },
                            label = { Text("Search by Date (YYYY-MM-DD)") },
                            readOnly = true,
                            trailingIcon = {
                                Icon(Icons.Default.DateRange, contentDescription = null)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { showDatePicker = true }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // ——————————————————————————————
                        // 5) SEARCH BUTTON
                        // ——————————————————————————————
                        Button(
                            onClick = { vm.search(token) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text("Search Activity")
                        }
                    }
                }
            }
        } // end of Card for filters

        Spacer(modifier = Modifier.height(8.dp))

        // ─────────────────────────────────────────────────────────────────────────
        // 6) RESULTS / LOADING / ERROR
        // This LazyColumn now uses Modifier.weight(1f) so it fills whatever
        // space is left after the (collapsible) filter panel.
        // ─────────────────────────────────────────────────────────────────────────
        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            state.fullResults.isEmpty() && !state.loading -> {
                // No results at all (either never searched or nothing matched).
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No activities found")
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp, horizontal = 0.dp)
                ) {
                    // Show only the current "page" of results:
                    items(state.visibleResults, key = { it.id }) { activity ->
                        ActivityCard(
                            activity = activity,
                            bgColor = Color(0xFFF5F5F5),
                            onClick = { onActivityClick(activity) }
                            // The defaults for bgColor, bgBrush, etc. are used.
                        )
                    }

                    // "Load more" button if there's another page
                    if (state.hasMore) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { vm.loadMore() },
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
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // 7) QUERY GOOGLE PLACES FOR LOCATION SUGGESTIONS
    // ─────────────────────────────────────────────────────────────────────────────
    LaunchedEffect(state.place) {
        val queryText = state.place
        if (queryText.isNotBlank()) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setSessionToken(sessionToken)
                .setTypeFilter(TypeFilter.CITIES)
                .setQuery(queryText)
                .build()
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    locationSuggestions = response.autocompletePredictions
                }
                .addOnFailureListener {
                    locationSuggestions = emptyList()
                }
        } else {
            locationSuggestions = emptyList()
        }
    }
}
