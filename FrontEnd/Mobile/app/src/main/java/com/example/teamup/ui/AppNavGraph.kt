package com.example.teamup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.teamup.data.remote.Repository.AuthRepositoryImpl
import com.example.teamup.data.domain.usecase.LoginUseCase
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.ui.screens.*
import com.example.teamup.ui.screens.Activity.EditActivityScreen
import com.example.teamup.ui.screens.Chat.ChatDetailScreen
import com.example.teamup.ui.screens.main.UserManager.LoginViewModel
import com.example.teamup.ui.screens.main.UserManager.LoginViewModelFactory
import com.example.teamup.ui.components.RootScaffold
import com.example.teamup.ui.screens.Profile.PublicProfileScreen
import com.example.teamup.ui.screens.main.UserManager.ForgotPasswordScreen
import com.example.teamup.ui.screens.main.UserManager.ForgotPasswordViewModel
import com.example.teamup.ui.screens.main.UserManager.LoginScreen
import com.example.teamup.ui.screens.main.UserManager.RegisterScreen
import com.example.teamup.ui.screens.main.UserManager.RegisterViewModel
import com.example.teamup.ui.screens.main.UserManager.RegisterViewModelFactory
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()

    NavHost(navController = nav, startDestination = "login") {

        /* ─── 1) LOGIN ─────────────────────────────────────────── */
        composable("login") {
            // Create / remember a LoginViewModel via factory
            val loginVM: LoginViewModel = viewModel(
                factory = remember {
                    val repo = AuthRepositoryImpl(AuthApi.create())
                    val useCase = LoginUseCase(repo)
                    LoginViewModelFactory(useCase)
                }
            )

            LoginScreen(
                loginViewModel = loginVM,
                onForgotPasswordClick = {
                    // Navigate to the “forgot password” screen
                    nav.navigate("forgot_password")
                },
                onRegisterClick = {
                    // Navigate to the “register” screen
                    nav.navigate("register")
                },
                onLoginSuccess = { token ->
                    // After login success, go to Splash (or main) – encode token in route
                    val encoded = java.net.URLEncoder.encode(token, "UTF-8")
                    nav.navigate("splash/$encoded") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        /* ─── 2) FORGOT PASSWORD ───────────────────────────────── */
        composable("forgot_password") {
            val forgotVM: ForgotPasswordViewModel = viewModel()
            ForgotPasswordScreen(
                forgotPasswordViewModel = forgotVM,
                onBack = {
                    // Go back to the login screen
                    nav.popBackStack("login", inclusive = false)
                },
                onResetLinkSent = {
                    // After success, pop back to login
                    nav.popBackStack("login", inclusive = false)
                }
            )
        }
        /* ─── Register ─────────────────────────────────────────────────────── */
        composable("register") {
            val registerVM: RegisterViewModel = viewModel(
                factory = remember { RegisterViewModelFactory() }
            )

            RegisterScreen(
                registerViewModel = registerVM,
                onBackToLogin = { nav.popBackStack("login", inclusive = false) },
                onRegistrationDone = { nav.popBackStack("login", inclusive = false) }


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


    }
}
