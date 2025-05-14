package com.example.teamup.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.teamup.R
import com.example.teamup.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(start: String = "chats") {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()

    /* -------- Scaffold base -------- */
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
                    IconButton(onClick = {}) {
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
                        selected = backStack?.destination?.route == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
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
            navController = navController,
            startDestination = start,
            modifier = Modifier.padding(padding)
        ) {
            composable("home")   { HomeScreen() }
            composable("agenda") { ativityScreen() }
            composable("chats")  { UpChatScreens() }
            composable("perfil") { PerfilScreen() }
        }
    }
}

private data class NavItem(val route: String, val icon: Int)
