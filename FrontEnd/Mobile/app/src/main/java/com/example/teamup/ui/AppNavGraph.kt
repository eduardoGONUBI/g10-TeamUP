/* ─── app/src/main/java/com/example/teamup/ui/AppNavGraph.kt ──────────────── */
package com.example.teamup.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.teamup.data.remote.Repository.AuthRepositoryImpl
import com.example.teamup.data.domain.usecase.LoginUseCase
import com.example.teamup.data.remote.api.AuthApi
import com.example.teamup.data.local.AppDatabase
import com.example.teamup.data.local.SessionRepository
import com.example.teamup.ui.components.RootScaffold
import com.example.teamup.ui.screens.*
import com.example.teamup.ui.screens.Activity.EditActivityScreen
import com.example.teamup.ui.screens.ChatDetailScreen
import com.example.teamup.ui.screens.Profile.PublicProfileScreen
import com.example.teamup.ui.screens.main.UserManager.*
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavGraph(
    navController: NavHostController = rememberNavController()      // default for previews/tests
) {
    val nav = navController  // alias preserves existing variable names

    /* ───── SessionRepository scoped to this NavGraph ───── */
    val context = LocalContext.current
    val sessionRepo = remember {
        SessionRepository(AppDatabase.get(context).sessionDao())
    }

    NavHost(navController = nav, startDestination = "login") {

        /* ─── 1) LOGIN ─────────────────────────────────────────── */
        composable("login") {
            val loginVM: LoginViewModel = viewModel(
                factory = remember(sessionRepo) {
                    val repo    = AuthRepositoryImpl(AuthApi.create())
                    val useCase = LoginUseCase(repo)
                    LoginViewModelFactory(useCase, sessionRepo)    // pass SessionRepository ✅
                }
            )

            LoginScreen(
                loginViewModel = loginVM,
                onForgotPasswordClick = { nav.navigate("forgot_password") },
                onRegisterClick       = { nav.navigate("register") },
                onLoginSuccess = { token ->
                    val encoded = URLEncoder.encode(token, "UTF-8")
                    nav.navigate("splash/$encoded") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }

        /* ─── 2) FORGOT PASSWORD ──────────────────────────────── */
        composable("forgot_password") {
            val forgotVM: ForgotPasswordViewModel = viewModel()
            ForgotPasswordScreen(
                forgotPasswordViewModel = forgotVM,
                onBack          = { nav.popBackStack("login", inclusive = false) },
                onResetLinkSent = { nav.popBackStack("login", inclusive = false) }
            )
        }

        /* ─── 3) REGISTER ─────────────────────────────────────── */
        composable("register") {
            val registerVM: RegisterViewModel = viewModel(
                factory = remember { RegisterViewModelFactory() }
            )

            RegisterScreen(
                registerViewModel = registerVM,
                onBackToLogin      = { nav.popBackStack("login", inclusive = false) },
                onRegistrationDone = { nav.popBackStack("login", inclusive = false) }
            )
        }

        /* ─── 4) SPLASH ───────────────────────────────────────── */
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

        /* ─── 5) MAIN SCAFFOLD ───────────────────────────────── */
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

        /* ─── 6) CHAT DETAIL (real-time) ─────────────────────── */
        composable(
            route = "chatDetail/{chatTitle}/{eventId}/{token}/{myUserId}",
            arguments = listOf(
                navArgument("chatTitle") { type = NavType.StringType },
                navArgument("eventId")   { type = NavType.IntType    },
                navArgument("token")     { type = NavType.StringType },
                navArgument("myUserId")  { type = NavType.IntType    }
            )
        ) { back ->
            val title     = back.arguments!!.getString("chatTitle")!!
            val eventId   = back.arguments!!.getInt("eventId")
            val tokenEnc  = back.arguments!!.getString("token")!!
            val token     = URLDecoder.decode(tokenEnc, StandardCharsets.UTF_8.toString())
            val myUserId  = back.arguments!!.getInt("myUserId")

            ChatDetailScreen(
                chatTitle = title,
                eventId   = eventId,
                token     = token,
                myUserId  = myUserId,
                onBack    = { nav.popBackStack() }
            )
        }

        /* ─── 7) EDIT ACTIVITY (deep link) ───────────────────── */
        composable(
            route = "edit_activity/{eventId}/{token}",
            arguments = listOf(
                navArgument("eventId") { type = NavType.IntType    },
                navArgument("token")   { type = NavType.StringType }
            )
        ) { back ->
            val id    = back.arguments!!.getInt("eventId")
            val token = URLDecoder.decode(
                back.arguments!!.getString("token")!!,
                StandardCharsets.UTF_8.toString()
            )

            EditActivityScreen(
                eventId   = id,
                token     = token,
                onBack    = { nav.popBackStack() },
                onSave    = { nav.popBackStack() },
                onDelete  = { nav.popBackStack() }
            )
        }
    }
}
