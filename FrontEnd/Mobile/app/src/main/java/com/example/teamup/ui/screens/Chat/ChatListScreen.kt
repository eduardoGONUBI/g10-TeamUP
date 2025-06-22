// File: app/src/main/java/com/example/teamup/ui/screens/Chat/ChatListScreen.kt
package com.example.teamup.ui.screens.Chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.teamup.domain.repository.ActivityRepository
import com.example.teamup.data.remote.repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.domain.model.Chat
import com.example.teamup.ui.components.ChatCard
import kotlinx.coroutines.launch
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun ChatListScreen(
    navController: NavController,
    token: String,
    myUserId: Int
) {
    // 1) ViewModel setup
    val vm: ChatListScreenViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo: ActivityRepository = ActivityRepositoryImpl(ActivityApi.create())
                return ChatListScreenViewModel(repo) as T
            }
        }
    )

    // 2) Initial load
    LaunchedEffect(token) {
        vm.loadFirstPage("Bearer $token")
    }

    // 3) Observe UI state
    val ui by vm.state.collectAsState()

    // 4) Pager & Tabs setup
    val pagerState: PagerState = rememberPagerState(pageCount = { 2 })
    val coroutineScope = rememberCoroutineScope()
    val titles = listOf("Chats", "Archive")

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pagerState.currentPage) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick  = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                    text     = { Text(title) }
                )
            }
        }

        HorizontalPager(
            state    = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIdx ->
            // Determine active vs archive
            val visibleList = if (pageIdx == 0) ui.visibleActive else ui.visibleArchive
            val hasMore     = if (pageIdx == 0) ui.hasMoreActive  else ui.hasMoreArchive

            when {
                // Loading state
                ui.loading && visibleList.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                // Error state
                ui.error != null && visibleList.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${ui.error}")
                    }
                }
                // Empty list state
                visibleList.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No chats.")
                    }
                }
                // Content
                else -> {
                    ChatListColumn(
                        chats      = visibleList,
                        hasMore    = hasMore,
                        onLoadMore = {
                            if (pageIdx == 0) vm.loadMoreActive() else vm.loadMoreArchive()
                        },
                        onClick    = { chat ->
                            val route = buildString {
                                append("chatDetail/")
                                append(
                                    URLEncoder.encode(
                                        chat.title,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                )
                                append("/")
                                append(chat.id)
                                append("/")
                                append(
                                    URLEncoder.encode(
                                        token,
                                        StandardCharsets.UTF_8.toString()
                                    )
                                )
                                append("/")
                                append(myUserId)
                            }
                            navController.navigate(route)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChatListColumn(
    chats: List<Chat>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onClick: (Chat) -> Unit
) {
    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(chats, key = { it.id }) { chat ->
            ChatCard(
                chat    = chat,
                onClick = { onClick(chat) }
            )
        }
        if (hasMore) {
            item {
                Box(
                    modifier         = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Button(onClick = onLoadMore) {
                        Text("Load more")
                    }
                }
            }
        }
    }
}
