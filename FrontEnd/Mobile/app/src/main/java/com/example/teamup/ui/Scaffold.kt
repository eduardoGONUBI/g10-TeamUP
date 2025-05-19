package com.example.teamup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.teamup.R
import com.example.teamup.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(
    appNav: NavHostController,       // controlador global (AppNavGraph)
    start: String = "chats"
) {
    // controlador interno para as tabs E notificações
    val navController = rememberNavController()
    val backStack     by navController.currentBackStackEntryAsState()
    val currentRoute  = backStack?.destination?.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Icon(
                        painter = painterResource(R.drawable.ic_launcher_foreground),
                        contentDescription = "Logo",
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                    )
                },
                actions = {
                    // 🔔 usa navController, não appNav
                    IconButton(onClick = { navController.navigate("notifications") }) {
                        Icon(
                            painter = painterResource(R.drawable.baseline_access_alarm_24),
                            contentDescription = "Notificações"
                        )
                    }
                }
            )
        },

        bottomBar = {
            NavigationBar {
                val items = listOf(
                    NavItem("home",   R.drawable.baseline_home_24),
                    NavItem("agenda", R.drawable.baseline_calendar_month_24),
                    NavItem("chats",  R.drawable.baseline_chat_bubble_24),
                    NavItem("perfil", R.drawable.baseline_person_24)
                )
                items.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            // 1. Se a rota no topo é "notifications", remove-a
                            if (currentRoute == "notifications") {
                                navController.popBackStack("notifications", inclusive = true)
                            }
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.id) {
                                    saveState = true
                                    inclusive = false
                                }
                                launchSingleTop = true
                                restoreState    = true
                            }
                        },
                        icon  = { Icon(painterResource(item.icon), contentDescription = item.route) },
                        label = { Text(item.route.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

    ) { padding ->
        NavHost(
            navController    = navController,
            startDestination = start,
            modifier         = Modifier.padding(padding)
        ) {
            composable("home")          { HomeScreen() }
            composable("agenda")        { ativityScreen() }
            // o "chats" usa appNav para saltar para chatDetail
            composable("chats")         { UpChatScreens(navController = appNav) }
            composable("perfil")        { PerfilScreen() }
            composable("notifications") { NotificationScreen() }
        }
    }
}

private data class NavItem(val route: String, val icon: Int)
