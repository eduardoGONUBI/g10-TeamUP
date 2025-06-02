package com.example.teamup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamup.data.remote.AuthRepositoryImpl
import com.example.teamup.data.domain.usecase.LoginUseCase
import com.example.teamup.data.remote.AuthApi
import com.example.teamup.ui.screens.*
import com.example.teamup.ui.screens.Activity.EditActivityScreen
import com.example.teamup.ui.screens.Chat.ChatDetailScreen
import com.example.teamup.ui.screens.main.Main.LoginViewModel
import com.example.teamup.ui.screens.main.Main.LoginViewModelFactory
import com.example.teamup.ui.components.RootScaffold
import com.example.teamup.ui.screens.Activity.CreatorActivityScreen
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "login") {

        /* ─── Login ───────────────────────────────────────────── */
        composable("login") {
            val loginVM: LoginViewModel = viewModel(
                factory = remember {
                    val repo   = AuthRepositoryImpl(AuthApi.create())
                    val useCase = LoginUseCase(repo)
                    LoginViewModelFactory(useCase)
                }
            )

            LoginScreen(
                loginViewModel = loginVM,
                onLoginSuccess = { token ->
                    val enc = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
                    nav.navigate("splash/$enc") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onForgotPasswordClick = {},
                onRegisterClick      = {}
            )
        }

        /* ─── Splash ──────────────────────────────────────────── */
        composable(
            route = "splash/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { back ->
            val enc   = back.arguments!!.getString("token")!!
            val token = URLDecoder.decode(enc, StandardCharsets.UTF_8.toString())

            SplashLogoScreen(
                onContinue = {
                    val re = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
                    nav.navigate("main/$re") {
                        popUpTo("splash/$enc") { inclusive = true }
                    }
                }
            )
        }

        /* ─── Main scaffold ───────────────────────────────────── */
        composable(
            route = "main/{token}",
            arguments = listOf(navArgument("token") { type = NavType.StringType })
        ) { back ->
            val token = URLDecoder.decode(
                back.arguments!!.getString("token")!!,
                StandardCharsets.UTF_8.toString()
            )

            RootScaffold(appNav = nav, token = token)
        }

        /* ─── Chats detail (global) ───────────────────────────── */
        composable(
            route = "chatDetail/{chatTitle}",
            arguments = listOf(navArgument("chatTitle") { type = NavType.StringType })
        ) { back ->
            val title = back.arguments!!.getString("chatTitle")!!
            ChatDetailScreen(
                chatTitle = title,
                onBack    = { nav.popBackStack() }
            )
        }

        /* ─── Creator activity (deep-link) ───────────────────── */
        composable(
            route = "creator_activity/{eventId}/{token}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType },
                navArgument("token")   { type = NavType.StringType }
            )
        ) { back ->
            val id    = back.arguments!!.getInt("eventId")
            val token = URLDecoder.decode(
                back.arguments!!.getString("token")!!,
                StandardCharsets.UTF_8.toString()
            )

            CreatorActivityScreen(
                eventId = id,
                token   = token,
                onBack  = { nav.popBackStack() },
                onEdit  = { eid ->
                    val re = URLEncoder.encode(token, StandardCharsets.UTF_8.toString())
                    nav.navigate("edit_activity/$eid/$re")
                }
            )
        }

        /* ─── Edit activity (deep-link) ──────────────────────── */
        composable(
            route = "edit_activity/{eventId}/{token}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType },
                navArgument("token")   { type = NavType.StringType }
            )
        ) { back ->
            val id    = back.arguments!!.getInt("eventId")
            val token = URLDecoder.decode(
                back.arguments!!.getString("token")!!,
                StandardCharsets.UTF_8.toString()
            )

            EditActivityScreen(
                eventId = id,
                token   = token,
                onBack   = { nav.popBackStack() },
                onSave   = { nav.popBackStack() },
                onDelete = { nav.popBackStack() }
            )
        }

        /* ─── Edit profile (no params) ───────────────────────── */
        composable("edit_profile") {
            EditProfileScreen(
                onBack = { nav.popBackStack() }
            )
        }


    }
}
