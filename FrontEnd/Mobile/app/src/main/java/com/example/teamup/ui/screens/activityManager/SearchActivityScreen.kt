// app/src/main/java/com/example/teamup/ui/screens/activityManager/SearchActivityScreen.kt
package com.example.teamup.ui.screens.activityManager

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.domain.repository.ActivityRepository
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl
import com.example.teamup.ui.components.ActivityCard

@Composable
fun SearchActivityScreen(
    token: String,
    onActivityClick: (ActivityItem) -> Unit
) {
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

    // 3) Observe state
    val state by vm.state.collectAsState()

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

        FilterField(
            label = "Search by Name",
            value = state.name,
            onChange = { vm.updateFilter("name", it) }
        )
        FilterField(
            label = "Search by Sport",
            value = state.sport,
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            onChange = { vm.updateFilter("sport", it) }
        )
        FilterField(
            label = "Search by Location",
            value = state.place,
            trailingIcon = { Icon(Icons.Default.Place, contentDescription = null) },
            onChange = { vm.updateFilter("place", it) }
        )
        FilterField(
            label = "Search by Date (YYYY-MM-DD)",
            value = state.date,
            trailingIcon = { Icon(Icons.Default.DateRange, contentDescription = null) },
            onChange = { vm.updateFilter("date", it) }
        )

        Button(
            onClick = { vm.search(token) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Text("Search Activity")
        }

        Text(
            text = "Available Activities:",
            style = MaterialTheme.typography.titleMedium
        )

        when {
            state.loading -> {
                Box(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Text(
                    text = "Error: ${state.error}",
                    color = Color.Red,
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
                             onClick  = { onActivityClick(activity) } ,
                                     bgColor = Color(0xFFF5F5F5),
                         bgBrush = null  // ensures no gradient is applied
                                 )
                }
            }
        }
    }
}

@Composable
private fun FilterField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    trailingIcon: @Composable (() -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        trailingIcon = trailingIcon,
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    )
}
