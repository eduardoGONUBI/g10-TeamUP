// app/src/main/java/com/example/teamup/ui/components/RootScaffold.kt
package com.example.teamup.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.util.Base64
import org.json.JSONObject
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamup.R
import com.example.teamup.data.domain.model.ActivityItem
import com.example.teamup.data.remote.api.ActivityApi
import com.example.teamup.data.remote.Repository.ActivityRepositoryImpl
import com.example.teamup.data.remote.model.StatusUpdateRequest
import com.example.teamup.presentation.profile.ProfileViewModel
import com.example.teamup.ui.screens.*
import com.example.teamup.ui.screens.Activity.ActivityScreen
import com.example.teamup.ui.screens.Activity.EditActivityScreen
import com.example.teamup.ui.screens.ActivityDetailViewModel.ActivityRole
import com.example.teamup.ui.screens.Chat.ChatListScreen
import com.example.teamup.ui.screens.Profile.PublicProfileScreen
import com.example.teamup.ui.screens.activityManager.ActivityTabsScreen
import com.example.teamup.ui.screens.main.UserManager.ChangeEmailScreen
import com.example.teamup.ui.screens.main.UserManager.ChangeEmailViewModel
import com.example.teamup.ui.screens.main.UserManager.ChangePasswordScreen
import com.example.teamup.ui.screens.main.UserManager.ChangePasswordViewModel
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/* ───────────────────────── Root scaffold ───────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootScaffold(
    appNav: NavHostController,
    token: String,
    startRoute: String = "home"
) {
    // 1) Create a NavController for this scaffold
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    // 2) Instantiate HomeViewModel via ActivityRepositoryImpl
    val homeViewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val repo = ActivityRepositoryImpl(ActivityApi.create())
                return HomeViewModel(repo) as T
            }
        }
    )

    // 3) Observe current route for bottom‐nav highlighting
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    /* ── Parse myUserId from the JWT once ─────────────────────── */
    val myUserId: Int = remember(token) {
        fun extractId(jwt: String): Int? = runCatching {
            val payload = jwt.split(".")[1]
            val json    = String(Base64.decode(payload, Base64.URL_SAFE or Base64.NO_WRAP))
            JSONObject(json).getString("sub").toInt()
        }.getOrNull()

        extractId(token) ?: -1   // fallback if parsing fails
    }

    Scaffold(
        topBar = {
            TopBar(navController, token, homeViewModel)
        },
        bottomBar = {
            BottomNavigationBar(
                navController = navController,
                currentRoute = currentRoute,
                token        = token // needed for “Profile” route
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
                    onActivityClick = { activity ->
                        val encoded = URLEncoder.encode(token, "UTF-8")
                        when {
                            activity.isCreator      -> navController.navigate("creator_activity/${activity.id}/$encoded")
                            activity.isParticipant  -> navController.navigate("participant_activity/${activity.id}/$encoded")
                            else                    -> navController.navigate("viewer_activity/${activity.id}/$encoded")
                        }
                    }
                )
            }

            /* ─── Activities (three‐tab host) ─────────────────── */
            composable("agenda") {
                ActivityTabsScreen(
                    token = token,
                    onActivityClick = { activity ->
                        val encoded = URLEncoder.encode(token, "UTF-8")
                        when {
                            activity.isCreator      -> navController.navigate("creator_activity/${activity.id}/$encoded")
                            activity.isParticipant  -> navController.navigate("participant_activity/${activity.id}/$encoded")
                            else                    -> navController.navigate("viewer_activity/${activity.id}/$encoded")
                        }
                    }
                )
            }



            /* ─── Profile (token in route) ─────────────────────── */
            composable(
                route = "perfil/{token}",
                arguments = listOf(navArgument("token") { type = NavType.StringType })
            ) { back ->
                val encoded = back.arguments!!.getString("token")!!
                val decoded = URLDecoder.decode(encoded, "UTF-8")

                ProfileScreen(
                    token = decoded,

                    onEditProfile = {
                        val e = URLEncoder.encode(decoded, "UTF-8")
                        navController.navigate("edit_profile/$e/$myUserId")
                    },

                    onLogout = {
                        appNav.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    },

                    onActivityClick = { activityItem ->
                        activityItem.id.toIntOrNull()?.let { eventId ->
                            val e = URLEncoder.encode(decoded, "UTF-8")
                            navController.navigate("viewer_activity/$eventId/$e")
                        }
                    }
                )
            }

            /* ─── Edit profile ───────────────────────────────────────────── */
            composable(
                "edit_profile/{token}/{uid}",
                arguments = listOf(navArgument("token") { type = NavType.StringType })

            ) { back ->
                val raw     = back.arguments!!.getString("token")!!
                val decoded = URLDecoder.decode(raw, "UTF-8")

                val uid      = back.arguments!!.getInt("uid")

                // You could fetch current values first; here we pass blanks for brevity
                EditProfileScreen(
                    token            = decoded,
                    userId           = uid,
                    usernameInitial  = "",      // pre-fill when you have them
                    locationInitial  = "",
                    sportsInitial    = emptyList(),
                    onFinished       = {        // success OR deleted
                        navController.popBackStack()
                        if (it /*deleted*/ == true) {
                            appNav.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onBack           = { navController.popBackStack() },
                    onChangePassword = {
                        val e = URLEncoder.encode(decoded, "UTF-8")
                        navController.navigate("change_password/$e")
                    },
                    onChangeEmail = {
                        val e = URLEncoder.encode(decoded, "UTF-8")
                        navController.navigate("change_email/$e")
                    }

                )
            }

            /* ─── CHANGE PASSWORD (nested under Profile) ────────────────────────── */
            composable(
                route = "change_password/{token}",
                arguments = listOf(navArgument("token") { type = NavType.StringType })
            ) { backStackEntry ->
                val rawToken = backStackEntry.arguments!!.getString("token")!!
                val token    = URLDecoder.decode(rawToken, "UTF-8")

                val changeVm: ChangePasswordViewModel = viewModel()
                ChangePasswordScreen(
                    changePasswordViewModel = changeVm,
                    token                   = token,
                    onBack                  = { navController.popBackStack() },
                    onPasswordChanged       = { navController.popBackStack() }
                )
            }
            /* ─── CHANGE EMAIL ──────────────────────────────────────────────── */
            composable(
                route = "change_email/{token}",
                arguments = listOf(navArgument("token") { type = NavType.StringType })
            ) { back ->
                val rawToken = back.arguments!!.getString("token")!!
                val decoded = URLDecoder.decode(rawToken, "UTF-8")

                val changeEmailVm: ChangeEmailViewModel = viewModel()
                ChangeEmailScreen(
                    changeEmailViewModel = changeEmailVm,
                    token                 = decoded,
                    onBack                = { navController.popBackStack() },
                    onEmailChanged        = { navController.popBackStack() }
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


            /* ─── Creator activity ─────────────────────────────── */
            composable(
                "creator_activity/{eventId}/{token}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.IntType },
                    navArgument("token")   { type = NavType.StringType }
                )
            ) { back ->
                val eventId = back.arguments!!.getInt("eventId")
                val decodedToken = URLDecoder.decode(back.arguments!!.getString("token")!!, "UTF-8")
                val reEncoded   = URLEncoder.encode(decodedToken, "UTF-8")

                ActivityScreen(
                    eventId = eventId,
                    token = decodedToken,
                    role = ActivityRole.CREATOR,
                    onBack = { navController.popBackStack() },
                    onEdit = {
                        navController.navigate("edit_activity/$eventId/$reEncoded")
                    },
                    onCancel = {
                        navController.popBackStack()
                    },
                    onConclude = {
                        // ← replace LaunchedEffect with scope.launch
                        scope.launch {
                            val resp = ActivityApi.create()
                                .concludeByCreator("Bearer $decodedToken", eventId)
                            if (resp.isSuccessful) {
                                navController.popBackStack()
                            } else {
                                println("Conclude failed: ${resp.code()}")
                            }
                        }
                    },
                    onReopen = {
                        scope.launch {
                            val body = StatusUpdateRequest(status = "in progress")
                            val resp = ActivityApi.create()
                                .updateStatus("Bearer $decodedToken", eventId, body)
                            if (resp.isSuccessful) {
                                navController.popBackStack()
                            } else {
                                println("Re-open failed: ${resp.code()}")
                            }
                        }
                    },
                    onUserClick = { userId ->
                        navController.navigate("public_profile/$userId/$reEncoded")
                    }
                )
            }

            /* ─── Viewer activity ──────────────────────────────── */
            composable(
                "viewer_activity/{eventId}/{token}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.IntType },
                    navArgument("token")   { type = NavType.StringType }
                )
            ) { back ->
                val eventId = back.arguments!!.getInt("eventId")
                val decodedToken = URLDecoder.decode(back.arguments!!.getString("token")!!, "UTF-8")
                val reEncoded    = URLEncoder.encode(decodedToken, "UTF-8")

                ActivityScreen(
                    eventId = eventId,
                    token = decodedToken,
                    role = ActivityRole.VIEWER,
                    onBack = { navController.popBackStack() },
                    onJoin  = {
                        scope.launch {
                            val resp = ActivityApi.create().joinEvent("Bearer $decodedToken", eventId)
                            if (resp.isSuccessful) {
                                // Instead of popping back, open the Participant screen:
                                navController.navigate("participant_activity/$eventId/$reEncoded") {
                                    // remove the "viewer_activity" from the back stack so
                                    // back button doesn’t go back to “viewer” (optional):
                                    popUpTo("viewer_activity/$eventId/$reEncoded") { inclusive = true }
                                }
                            } else {
                                println("Join failed: ${resp.code()}")
                            }
                        }
                              },
                    onUserClick = { userId ->
                        navController.navigate("public_profile/$userId/$reEncoded")
                    }
                )
            }

            /* ─── Participant activity ─────────────────────────────── */
            composable(
                "participant_activity/{eventId}/{token}",
                arguments = listOf(
                    navArgument("eventId") { type = NavType.IntType },
                    navArgument("token")   { type = NavType.StringType }
                )
            ) { back ->
                val eventId = back.arguments!!.getInt("eventId")
                val decodedToken = URLDecoder.decode(back.arguments!!.getString("token")!!, "UTF-8")
                val reEncoded    = URLEncoder.encode(decodedToken, "UTF-8")

                ActivityScreen(
                    eventId = eventId,
                    token = decodedToken,
                    role = ActivityRole.PARTICIPANT,
                    onBack = { navController.popBackStack() },
                    onLeave = {
                        scope.launch {
                            val resp = ActivityApi.create()
                                .leaveEvent("Bearer $decodedToken", eventId)
                            if (resp.isSuccessful) {
                                navController.popBackStack()
                            } else {
                                println("Leave failed: ${resp.code()}")
                            }
                        }
                    },
                    onUserClick = { userId ->
                        navController.navigate("public_profile/$userId/$reEncoded")
                    }
                )
            }

            /* ─── Notifications ───────────────────────────────── */
            composable("notifications") {
                NotificationScreen()
            }

            /* ─── Public profile (any user can see) ────────────────── */
            composable(
                route = "public_profile/{uid}/{token}",
                arguments = listOf(
                    navArgument("uid")   { type = NavType.IntType },
                    navArgument("token") { type = NavType.StringType }
                )
            ) { back ->
                val uid = back.arguments!!.getInt("uid")
                val rawToken = back.arguments!!.getString("token")!!
                val token = URLDecoder.decode(rawToken, StandardCharsets.UTF_8.toString())

                PublicProfileScreen(
                    token = token,
                    userId = uid,
                    onBack = { navController.popBackStack() },
                    onEventClick = { activityItem ->
                        navController.navigate("viewer_activity/${activityItem.id}/$token")
                    }
                )
            }

            /* ─── Chat ────────────────── */
            composable("chats") {
                ChatListScreen(
                    navController = appNav,
                    token         = token,      //  ←  pass token
                    myUserId      = myUserId
                )
            }
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
    token: String
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
                            val destination = if (item.route == "perfil") "perfil/$encoded" else item.route
                            navController.navigate(destination) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    // Don’t save the previous state; always go to root
                                    saveState = false
                                }
                                // If already on this route, don’t add another copy
                                launchSingleTop = true
                                // Don’t restore any saved state
                                restoreState = false
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
