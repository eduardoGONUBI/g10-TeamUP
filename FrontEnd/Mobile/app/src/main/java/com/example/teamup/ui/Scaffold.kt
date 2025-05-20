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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.teamup.R
import com.example.teamup.ui.screens.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(appNav: NavHostController,startRoute: String = "Home") {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route


    val sampleActivities = listOf(
        Activity("1", "Futebolada : Football",   "Complexo Desportivo da Rodovia", "Feb 5, 5:00 PM", 14, 14),
        Activity("2", "BarcelosBasket : Basketball", "Escola Secundária de Barcelos", "Feb 6, 7:00 PM", 5, 10),
        Activity("3", "Semana Ténis 2 : Tennis","Parque Municipal de Barcelos",  "Feb 7, 11:00 AM", 2, 2)
    )

    Scaffold(
        topBar = { TopBar(navController) },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {
            composable("home") {
                HomeScreen(
                    activities      = sampleActivities,
                    onActivityClick = {navController.navigate("creator_activity")}
                )
            }
            composable("agenda") { AtivityScreen() }
            composable("chats")  { UpChatScreens(navController = appNav) }
            composable("perfil") { ProfileScreen() }
            composable("activityDetail") {
                EditActivityScreen(
                    onSave   = { navController.popBackStack() },
                    onDelete = { navController.popBackStack() }
                )
            }
            composable("notifications") {
                NotificationScreen()
            }
            composable("creator_activity") {
                CreatorActivityScreen(
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(navController: NavHostController) {
    Column {
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            navController.navigate("home") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                ) {
                    Image(
                        painter           = painterResource(id = R.drawable.icon_up),
                        contentDescription = "App Logo",
                        modifier          = Modifier.size(48.dp),
                        contentScale      = ContentScale.Fit
                    )
                }
            },
            actions = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { navController.navigate("notifications") }
                        .padding(12.dp)
                ) {
                    Icon(
                        painter           = painterResource(id = R.drawable.notifications),
                        contentDescription = "Notifications",
                        modifier          = Modifier.size(24.dp),
                        tint              = Color(0xFF335EB5)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
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
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?
) {
    data class NavItem(val route: String, val title: String, val drawableRes: Int)
    val items = listOf(
        NavItem("home",   "Home",       R.drawable.main),
        NavItem("agenda", "Activities", R.drawable.atividades),
        NavItem("chats",  "Chats",      R.drawable.chat),
        NavItem("perfil", "com/example/teamup/ui/screens/Profile",    R.drawable.profileuser)
    )
    val selectedColor = Color(0xFF3629B7)
    val unselectedColor = Color(0xFF023499)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 16.dp),
        color = Color.White
    ) {
        Column {
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
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route

                    Box(
                        modifier = Modifier
                            .clickable {
                                // para as notificaçoes nao prender o ultimo ecra
                                if (currentRoute == "notifications") {
                                    navController.popBackStack("notifications", inclusive = true)
                                }
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                    ) {
                        if (selected) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .background(
                                        color = selectedColor,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(id = item.drawableRes),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.title,
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        } else {
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

private data class NavItem(
    val route: String,
    val icon: ImageVector? = null,
    val drawableRes: Int? = null
)
