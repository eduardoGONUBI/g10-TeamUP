// app/src/main/java/com/example/teamup/ui/screens/activityManager/ActivityTabsScreen.kt
package com.example.teamup.ui.screens.activityManager

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.teamup.domain.model.Activity
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.repository.ActivityRepositoryImpl
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ActivityTabsScreen(
    token: String,
    onActivityClick: (Activity) -> Unit
) {
    // 1) Create a single repository instance
    val repo = remember { ActivityRepositoryImpl(ActivityApi.create()) }

    // 2) Hoist both ViewModels only once (so Compose doesn't recreate them on rotation, etc.)
    val searchVm: SearchActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SearchActivityViewModel(repo) as T
            }
        }
    )

    val createVm: CreateActivityViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreateActivityViewModel(repo) as T
            }
        }
    )

    val yourActivitiesVm: YourActivitiesViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return YourActivitiesViewModel(repo) as T
            }
        }
    )

    // 3) Pager with 3 pages
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            listOf("Search", "Create", "Your Activities").forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (pagerState.currentPage == index)
                                FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .padding(top = 8.dp)
        ) { page ->
            when (page) {
                0 -> SearchActivityScreen(
                    token = token,
                    onActivityClick = onActivityClick
                )
                1 -> CreateActivityScreen(
                    token = token,
                    onCreated = {
                        // As soon as creation succeeds, jump to the “Your Activities” tab (index 2)
                        scope.launch { pagerState.animateScrollToPage(2) }
                    }
                )
                2 -> YourActivitiesScreen(
                    token = token,

                    onActivityClick = onActivityClick
                )
            }
        }
    }
}

@Composable
private fun YourActivitiesPlaceholder() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("Your Activities screen coming soon.")
    }
}
