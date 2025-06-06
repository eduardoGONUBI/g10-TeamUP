// File: app/src/main/java/com/example/teamup/ui/screens/activityManager/SearchActivityScreen.kt
package com.example.teamup.ui.screens.activityManager

import android.app.DatePickerDialog
import android.annotation.SuppressLint
import android.widget.DatePicker
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
import java.time.ZoneId
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("StateFlowValueCalledInComposition")
@Composable
fun SearchActivityScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit
) {
    val context = LocalContext.current

    // 1) Initialize Google Places (replace with your actual API key)
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

    // 8) Load sports from backend once
    LaunchedEffect(Unit) {
        try {
            val fetched: List<SportDto> = repo.getSports(token)
            sportsList = fetched
        } catch (_: Exception) {
            // ignore or handle error
        }
    }

    // 9) Build an instance of DatePickerDialog when needed
    if (showDatePicker) {
        // Use Calendar to pick today's date as the default
        val todayCalendar = Calendar.getInstance()
        val year = todayCalendar.get(Calendar.YEAR)
        val month = todayCalendar.get(Calendar.MONTH)
        val day = todayCalendar.get(Calendar.DAY_OF_MONTH)

        DatePickerDialog(
            context,
            { _: DatePicker, pickedYear: Int, pickedMonth: Int, pickedDay: Int ->
                // Compose months are 0-based, so add 1
                val pickedDate = LocalDate.of(pickedYear, pickedMonth + 1, pickedDay)
                vm.updateFilter("date", pickedDate.toString())
                showDatePicker = false
            },
            year, month, day
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "Filters",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 8.dp)
        )

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
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(sportExpanded) },
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
                trailingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
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
        // 4) SEARCH BY DATE (opens native Android calendar)
        // ——————————————————————————————
        OutlinedTextField(
            value = state.date,
            onValueChange = { /* read-only */ },
            label = { Text("Search by Date (YYYY-MM-DD)") },
            readOnly = true,
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
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

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Available Activities:",
            style = MaterialTheme.typography.titleMedium
        )

        // ——————————————————————————————
        // 6) RESULTS / LOADING / ERROR
        // ——————————————————————————————
        when {
            state.loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )
            }
            state.results.isEmpty() -> {
                Text(
                    text = "No activities found",
                    modifier = Modifier.padding(24.dp)
                )
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(state.results, key = { it.id }) { activity ->
                    ActivityCard(
                        activity = activity,
                        onClick = { onActivityClick(activity) }
                    )
                }
            }
        }
    }

    // ——————————————————————————————
    // 7) QUERY GOOGLE PLACES FOR LOCATION SUGGESTIONS
    // ——————————————————————————————
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
