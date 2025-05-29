// RootScaffold.kt
package com.example.teamup.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamup.R
import com.example.teamup.data.remote.ActivityApi
import com.example.teamup.presentation.profile.ProfileViewModel
import com.example.teamup.ui.screens.*
import com.example.teamup.ui.screens.Ativity.EditActivityScreen
import com.example.teamup.ui.screens.Chat.UpChatScreens
import java.net.URLDecoder
import java.net.URLEncoder

/* ───────────────────────── Root scaffold ───────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(
    appNav: NavHostController,
    token: String,
    startRoute: String = "home"
) {
    val navController = rememberNavController()
    val homeViewModel = remember { HomeViewModel(ActivityApi.create()) }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    Scaffold(
        topBar = {
            TopBar(navController, token, homeViewModel)
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                token        = token                        // ← needed for perfil route
            )
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = startRoute,
            modifier = Modifier.padding(padding)
        ) {

            /* ─── Home ──────────────────────────────────────────── */
            composable("home") {
                HomeScreen(
                    token = token,
                    viewModel = homeViewModel,
                    onActivityClick = { activity ->
                        if (activity.isCreator) {
                            navController.navigate("creator_activity/${activity.id}")
                        } else {
                            navController.navigate("viewer_activity/${activity.id}")
                        }
                    }
                )
            }

            /* ─── Agenda & Chats placeholders ─────────────────── */
            composable("agenda") { /* … */ }
            composable("chats")  { UpChatScreens(navController = appNav) }

            /* ─── Profile (token in route) ─────────────────────── */
            composable(
                route = "perfil/{token}",
                arguments = listOf(navArgument("token") { type = NavType.StringType })
            ) { back ->
                val encoded = back.arguments!!.getString("token")!!
                val decoded = URLDecoder.decode(encoded, "UTF-8")

                val profileVM = remember { ProfileViewModel() }

                ProfileScreen(
                    token           = decoded,
                    viewModel       = profileVM,
                    onEditProfile   = {
                        val e = URLEncoder.encode(decoded, "UTF-8")
                        navController.navigate("edit_profile/$e")
                    },
                    onLogout        = {
                        // clear session…
                        navController.navigate("login")
                    },
                    onActivityClick = { id ->
                        val e = URLEncoder.encode(decoded, "UTF-8")
                        navController.navigate("viewer_activity/$id/$e")
                    }
                )
            }

            /* ─── Creator activity ─────────────────────────────── */
            composable(
                "creator_activity/{eventId}",
                arguments = listOf(navArgument("eventId") { type = NavType.IntType })
            ) { back ->
                val eventId = back.arguments!!.getInt("eventId")
                CreatorActivityScreen(
                    eventId = eventId,
                    token   = token,
                    onBack  = { navController.popBackStack() },
                    onEdit  = { id ->
                        val e = URLEncoder.encode(token, "UTF-8")
                        navController.navigate("edit_activity/$id/$e")
                    }
                )
            }

            /* ─── Viewer activity ──────────────────────────────── */
            composable(
                "viewer_activity/{eventId}/{token?}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.IntType },
                    navArgument("token")   { type = NavType.StringType; nullable = true }
                )
            ) { back ->
                val eventId = back.arguments!!.getInt("eventId")
                val tokArg  = back.arguments?.getString("token")
                val tok     = tokArg?.let { URLDecoder.decode(it, "UTF-8") } ?: token

                ViewerActivityScreen(
                    eventId = eventId,
                    token   = tok,
                    onBack  = { navController.popBackStack() }
                )
            }

            /* ─── Edit activity ───────────────────────────────── */
            composable(
                "edit_activity/{eventId}/{token}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.IntType },
                    navArgument("token")   { type = NavType.StringType }
                )
            ) { back ->
                val eventId = back.arguments!!.getInt("eventId")
                val decoded = URLDecoder.decode(back.arguments!!.getString("token")!!, "UTF-8")

                EditActivityScreen(
                    eventId  = eventId,
                    token    = decoded,
                    onBack   = { navController.popBackStack() },
                    onSave   = { navController.popBackStack() },
                    onDelete = { navController.popBackStack() }
                )
            }

            /* ─── Notifications ───────────────────────────────── */
            composable("notifications") { NotificationScreen() }
        }
    }
}

/* ───────────────────────── Top bar ───────────────────────────── */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    navController: NavHostController,
    token: String,
    viewModel: HomeViewModel
) {
    Column {
        TopAppBar(
            title = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable {
                            viewModel.loadActivities(token)
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.icon_up),
                        contentDescription = "Refresh",
                        modifier = Modifier.size(48.dp),
                        contentScale = ContentScale.Fit
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
                        painter = painterResource(id = R.drawable.notifications),
                        contentDescription = "Notifications",
                        modifier = Modifier.size(24.dp),
                        tint = Color(0xFF335EB5)
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
                        listOf(Color.Black.copy(alpha = 0.1f), Color.Transparent)
                    )
                )
        )
    }
}

/* ───────────────────── Bottom navigation ─────────────────────── */

@Composable
fun BottomNavigationBar(
    navController: NavHostController,
    currentRoute: String?,
    token: String          // ← need raw token to build perfil route
) {
    data class NavItem(val route: String, val title: String, val drawableRes: Int)
    val items = listOf(
        NavItem("home",   "Home",       R.drawable.main),
        NavItem("agenda", "Activities", R.drawable.atividades),
        NavItem("chats",  "Chats",      R.drawable.chat),
        NavItem("perfil", "Profile",    R.drawable.profileuser)
    )
    val selectedColor   = Color(0xFF3629B7)
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
                            listOf(Color.Black.copy(alpha = 0.1f), Color.Transparent)
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
                    val selected = currentRoute?.startsWith(item.route) == true
                    val encoded  = URLEncoder.encode(token, "UTF-8")

                    Box(
                        modifier = Modifier.clickable {
                            if (currentRoute == "notifications") {
                                navController.popBackStack("notifications", true)
                            }
                            navController.navigate(
                                if (item.route == "perfil") "perfil/$encoded"
                                else item.route
                            ) {
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
                                    .background(selectedColor, RoundedCornerShape(50))
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(item.drawableRes),
                                    contentDescription = item.title,
                                    modifier = Modifier.size(24.dp),
                                    tint = Color.White
                                )
                                Spacer(Modifier.width(8.dp))
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
                                painter = painterResource(item.drawableRes),
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
