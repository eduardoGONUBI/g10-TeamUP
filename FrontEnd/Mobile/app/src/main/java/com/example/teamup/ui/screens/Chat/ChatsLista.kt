package com.example.teamup.ui.screens.Chat

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch
import com.example.teamup.R

/* ---------- modelo + dados dummy ---------- */
// Data class simples para representar cada item de chat
private data class ChatItem(val title: String, val subtitle: String, val icon: Int)

// Lista de chats ativos (exemplo)
private val chats = listOf(
    ChatItem("Futebolada",       "Football",    R.drawable.baseline_sports_soccer_24),
    ChatItem("BarcelosBasket",   "Basketball",  R.drawable.baseline_sports_basketball_24),
    ChatItem("Semana Ténis 2",   "Tennis",      R.drawable.baseline_sports_tennis_24)
)

// Lista de chats arquivados (exemplo)
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
fun UpChatScreens(
    navController: NavHostController,  // Controller para navegar entre ecrãs
    modifier: Modifier = Modifier      // Modifier opcional para personalizar layout
) {
    // Estado do pager com 2 páginas (Chats e Archive)
    val pagerState = rememberPagerState(pageCount = { 2 })
    // Scope para lançar coroutines (animações de scroll)
    val scope = rememberCoroutineScope()
    // Títulos das tabs
    val tabs = listOf("Chats", "Archive")

    Column(modifier.fillMaxSize()) {  // Layout vertical que preenche o ecrã
        /* --- Tab bar --- */
        TabRow(selectedTabIndex = pagerState.currentPage) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,  // Destaca a tab selecionada
                    onClick  = {
                        // Ao clicar, anima o scroll até à página correspondente
                        scope.launch { pagerState.animateScrollToPage(index) }
                    },
                    text     = { Text(title) }  // Texto da tab
                )
            }
        }

        /* --- Pager (swipe) --- */
        HorizontalPager(
            state = pagerState,              // Estado do pager
            modifier = Modifier.fillMaxSize() // Preenche o espaço restante
        ) { page ->
            // Renderiza a lista correspondente a cada página
            when (page) {
                0 -> ChatList(chats, navController)     // Página 0: lista de chats ativos
                1 -> ChatList(archive, navController)   // Página 1: lista de chats arquivados
            }
        }
    }
}

/* ---------- lista simples de Chats ---------- */
@Composable
private fun ChatList(
    items: List<ChatItem>,           // Lista de itens a mostrar
    nav: NavHostController           // Controller para navegação
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),               // Lista rolável ocupa todo o ecrã
        contentPadding = PaddingValues(vertical = 8.dp)  // Espaçamento vertical interno
    ) {
        items(items) { chatItem ->
            ChatCard(chatItem, nav = nav)  // Para cada item, desenha um cartão
        }
    }
}

/* ---------- cartão de cada chat ---------- */
@Composable
private fun ChatCard(
    item: ChatItem,                  // Dados do chat
    nav: NavHostController           // Controller para navegação
) {
    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)  // Espaço à volta do card
            .clickable {
                // Navega para o detalhe do chat passando o título como argumento
                nav.navigate("chatDetail/${item.title}")
            }
            .fillMaxWidth(),                                 // Largura total
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)  // Sombra
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)          // Padding interno do row
                .fillMaxWidth(),         // Preenche largura do card
            verticalAlignment = Alignment.CenterVertically  // Alinha elementos verticalmente
        ) {
            // Ícone do chat
            Image(
                painter = painterResource(item.icon),
                contentDescription = null,
                modifier = Modifier.size(40.dp)  // Tamanho do ícone
            )
            Spacer(Modifier.width(12.dp))  // Espaço horizontal entre ícone e texto
            Column {
                // Título (nome do chat) em negrito
                Text(item.title, fontWeight = FontWeight.Bold)
                // Subtítulo (descrição breve)
                Text(item.subtitle, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
