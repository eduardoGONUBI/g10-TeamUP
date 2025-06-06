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

    // 1) Build repository
    val repo: ActivityRepository = remember {
        ActivityRepositoryImpl(ActivityApi.create())
    }

    // 2) Create ViewModel internally
    val vm: SearchActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchActivityViewModel(repo) as T
            }
        }
    )

    // 3) Kick off loading all events once
    LaunchedEffect(token) {
        vm.loadAllEvents(token)
    }

    // 4) Collect each piece of state separately
    val name by vm.name.collectAsState()
    val sport by vm.sport.collectAsState()
    val place by vm.place.collectAsState()
    val date by vm.date.collectAsState()
    val loading by vm.loading.collectAsState()
    val error by vm.error.collectAsState()
    val filtered by vm.filtered.collectAsState()
    val visibleResults by vm.visibleResults.collectAsState()
    val hasMore by vm.hasMore.collectAsState()

    // 5) Local UI state for Sports dropdown
    var sportsList by remember { mutableStateOf<List<SportDto>>(emptyList()) }
    var sportExpanded by remember { mutableStateOf(false) }

    // 6) Local UI state for showing DatePickerDialog
    var showDatePicker by remember { mutableStateOf(false) }

    // 7) Collapsible Filters state
    var filtersExpanded by remember { mutableStateOf(true) }

    // 8) Load sports from backend once
    LaunchedEffect(Unit) {
        try {
            val fetched: List<SportDto> = repo.getSports(token)
            sportsList = fetched
        } catch (_: Exception) {
            // ignore or log
        }
    }

    // 9) Show native DatePickerDialog if requested
    if (showDatePicker) {
        val today = Calendar.getInstance()
        DatePickerDialog(
            context,
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val pickedDate = LocalDate.of(year, month + 1, dayOfMonth)
                vm.updateFilter("date", pickedDate.toString())
                showDatePicker = false
            },
            today.get(Calendar.YEAR),
            today.get(Calendar.MONTH),
            today.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setOnCancelListener { showDatePicker = false }
        }.show()
    }

    // 10) Main UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ─────────────────────────────────────────────────────────────────
        // Collapsible Filter Panel
        // ─────────────────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
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
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // a) Search by Name
                        OutlinedTextField(
                            value = name,
                            onValueChange = { vm.updateFilter("name", it) },
                            label = { Text("Search by Name") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // b) Search by Sport (dropdown)
                        ExposedDropdownMenuBox(
                            expanded = sportExpanded,
                            onExpandedChange = { sportExpanded = !sportExpanded }
                        ) {
                            OutlinedTextField(
                                value = sport,
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

                        // c) Search by Location (simple text field)
                        OutlinedTextField(
                            value = place,
                            onValueChange = { vm.updateFilter("place", it) },
                            label = { Text("Search by Location") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // d) Search by Date (native DatePicker or type)
                        OutlinedTextField(
                            value = date,
                            onValueChange = { vm.updateFilter("date", it) },
                            label = { Text("Search by Date (YYYY-MM-DD)") },
                            singleLine = true,
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.DateRange,
                                    contentDescription = "Pick date",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clickable { showDatePicker = true }
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Optional: “Refresh Results” button
                        Button(
                            onClick = { vm.loadAllEvents(token) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                        ) {
                            Text("Refresh Results")
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // ─────────────────────────────────────────────────────────────────
        // Results / Loading / Error (fill remaining space)
        // ─────────────────────────────────────────────────────────────────
        when {
            loading -> {
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
            error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Error: $error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            filtered.isEmpty() && !loading -> {
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
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(visibleResults, key = { it.id }) { activity ->
                        ActivityCard(
                            activity = activity,
                            onClick = { onActivityClick(activity) }
                        )
                    }

                    if (hasMore) {
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
}
