// RootScaffold.kt
package com.example.teamup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.teamup.R
import com.example.teamup.ui.screens.*
import com.example.teamup.ui.screens.Ativity.AtivityScreen
import com.example.teamup.ui.screens.Ativity.EditActivityScreen
import com.example.teamup.ui.screens.Chat.UpChatScreens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(
    appNav: NavHostController,           // Controlador principal de navegação da app
    startRoute: String = "Home"          // Rota inicial dentro deste scaffold
) {
    // Cria um NavController local para navegar entre ecrãs dentro deste scaffold
    val navController = rememberNavController()

    // Observa a back stack atual para saber qual a rota ativa
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route  // Extrai o nome da rota ativa

    // Dados de exemplo: lista de atividades para passar ao HomeScreen
    val sampleActivities = listOf(
        Activity("1", "Futebolada : Football",   "Complexo Desportivo da Rodovia", "Feb 5, 5:00 PM", 14, 14),
        Activity("2", "BarcelosBasket : Basketball", "Escola Secundária de Barcelos", "Feb 6, 7:00 PM", 5, 10),
        Activity("3", "Semana Ténis 2 : Tennis",   "Parque Municipal de Barcelos",  "Feb 7, 11:00 AM", 2, 2)
    )

    // Scaffold define a estrutura base: topBar, bottomBar e content
    Scaffold(
        topBar = {
            TopBar(navController)  // Barra superior personalizada
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,  // Passa o controlador local
                currentRoute = currentRoute     // Para destacar o item selecionado
            )
        }
    ) { padding ->
        // Área de navegação interna ao scaffold, com o padding aplicado
        NavHost(
            navController = navController,
            startDestination = startRoute,    // Começa no ecrã definido
            modifier = Modifier.padding(padding)  // Garante não sobrepor barras
        ) {
            // Cada composable define um ecrã e a sua rota
            composable("home") {
                HomeScreen(
                    activities      = sampleActivities,
                    onActivityClick = { appNav.navigate("creator_activity") }
                )
            }
            composable("agenda") {
                AtivityScreen()  // Ecrã de agenda de atividades
            }
            composable("chats") {
                UpChatScreens(navController = appNav)  // Ecrã de chats, usando nav principal
            }
            composable("perfil") {
                ProfileScreen()  // Ecrã de perfil de utilizador
            }
            composable("activityDetail") {
                EditActivityScreen(
                    onSave = { navController.popBackStack() },   // Volta ao ecrã anterior após guardar
                    onDelete = { navController.popBackStack() }    // Volta após eliminar
                )
            }
            composable("notifications") {
                NotificationScreen()  // Ecrã de notificações
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavHostController) {
    Column {  // Empilha verticalmente TopAppBar e linha decorativa
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier
                        .size(48.dp)       // Define tamanho clicável
                        .clickable {       // Ação ao clicar no logo
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_up),  // Logótipo da app
                        contentDescription = "App Logo",
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .size(48.dp)       // Área de toque para o ícone de notificações
                        .clickable { navController.navigate("notifications") }
                        .padding(12.dp)    // Espaço interno para o ícone
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.notifications),
                        contentDescription = "Notifications",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF335EB5)  // Cor personalizada do ícone
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background  // Cor de fundo da barra
            )
        )
        // Linha de separação em degradé abaixo da TopAppBar
        Box(
            modifier = Modifier
                .fillMaxWidth()  // Ocupa toda a largura
                .height(4.dp)    // Altura fina
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.1f),  // Início semi-opaco
                            Color.Transparent               // Fim transparente
                        )
                    )
                )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?
) {
    // Data class local para representar cada item de navegação
    data class NavItem(val route: String, val title: String, val drawableRes: Int)

    // Lista de itens com rota, título e recurso de ícone
    val items = listOf(
        NavItem("home",   "Home",       R.drawable.main),
        NavItem("agenda", "Activities", R.drawable.atividades),
        NavItem("chats",  "Chats",      R.drawable.chat),
        NavItem("perfil", "Perfil",     R.drawable.profileuser)
    )

    // Cores para estado selecionado e não selecionado
    val selectedColor = Color(0xFF3629B7)
    val unselectedColor = Color(0xFF023499)

    Surface(
        modifier = Modifier
            .fillMaxWidth()             // Ocupa largura total
            .navigationBarsPadding()    // Padding para não sobrepor barras do SO
            .padding(bottom = 16.dp),   // Espaço extra abaixo
        color = Color.White           // Fundo branco
    ) {
        Column {
            // Linha de degradé acima do menu para efeito visual
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.1f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,  // Espaçar igualmente
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gera um item de navegação para cada NavItem
                items.forEach { item ->
                    val selected = currentRoute == item.route

                    Box(
                        modifier = Modifier
                            .clickable {
                                // Se vier das notificações, remove essa rota para não voltar a ela
                                if (currentRoute == "notifications") {
                                    navController.popBackStack("notifications", inclusive = true)
                                }
                                // Navega para a rota do item mantendo/restaurando estado
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                    ) {
                        if (selected) {
                            // Se o item estiver selecionado, desenha fundo colorido + ícone + texto
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = selectedColor,
                                        shape = RoundedCornerShape(50)  // Bordas arredondadas
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = item.drawableRes),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))  // Espaço entre ícone e texto
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis  // "..." se o texto for longo
                                )
                            }
                        } else {
                            // Se não estiver selecionado, mostra apenas o ícone
                            Icon(
                                painter = painterResource(id = item.drawableRes),
                                contentDescription = item.title,
                                modifier = Modifier.size(24.dp),
                                tint = unselectedColor
                            )
                        }
                    }
                }
            }
        }
    }
}

