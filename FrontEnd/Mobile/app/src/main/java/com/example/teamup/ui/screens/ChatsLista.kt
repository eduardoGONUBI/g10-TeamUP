package com.example.teamup.ui.screens
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.example.teamup.R

/* ---------- modelo e dados dummy ---------- */
private data class ChatItem(val title: String, val subtitle: String, val icon: Int)

private val chats = listOf(
    ChatItem("Futebolada",       "Football",    R.drawable.baseline_sports_soccer_24),
    ChatItem("BarcelosBasket",   "Basketball",  R.drawable.baseline_sports_basketball_24),
    ChatItem("Semana Ténis 2",   "Tennis",      R.drawable.baseline_sports_tennis_24)
)

private val archive = listOf(
    ChatItem("Semana 1 Ténis",   "Tennis",      R.drawable.baseline_sports_tennis_24),
    ChatItem("Treino Badminton", "Badminton",   R.drawable.baseline_cruelty_free_24),
    ChatItem("Associação Hockey","Hockey",      R.drawable.baseline_sports_soccer_24)
)
/* ------------------------------------------ */

/**
 * Ecrãs principais com TabRow + HorizontalPager
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun UpChatScreens(navController: NavHostController, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val tabs = listOf("Chats", "Archive")

    Column(modifier.fillMaxSize()) {
        /* --- Tab bar --- */
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick  = { scope.launch { pagerState.animateScrollToPage(index) } },
                    text     = { Text(title) }
                )
            }
        }

        /* --- Pager (swipe) --- */
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> ChatList(chats, navController)
                1 -> ChatList(archive, navController)
            }
        }
    }
}

/* ---------- lista simples de Chats ---------- */
@Composable
private fun ChatList(items: List<ChatItem>, nav: NavHostController) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(items) { ChatCard(it, nav=nav) }
    }
}

/* ---------- cartão de cada chat ---------- */
@Composable
private fun ChatCard(item: ChatItem,  nav: NavHostController  ) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clickable { nav.navigate("chatDetail/${item.title}") }
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(item.title, fontWeight = FontWeight.Bold)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

