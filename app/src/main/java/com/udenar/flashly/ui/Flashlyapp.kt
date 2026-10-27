package com.udenar.flashly.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.udenar.flashly.data.SupabaseProvider
import com.udenar.flashly.ui.auth.AuthScreen
import com.udenar.flashly.ui.create.CreateDeckScreen
import com.udenar.flashly.ui.decks.DecksScreen
import com.udenar.flashly.ui.game.GameScreen
import com.udenar.flashly.ui.history.HistoryScreen
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus

/** Según el estado de la sesión de Supabase muestra el login o la app. */
@Composable
fun FlashlyApp() {
    val estado by SupabaseProvider.client.auth.sessionStatus.collectAsState()
    when (estado) {
        is SessionStatus.Authenticated -> MainNav()
        is SessionStatus.Initializing -> Box(Modifier.fillMaxSize(), Alignment.Center) {
            CircularProgressIndicator()
        }
        else -> AuthScreen()
    }
}

@Composable
private fun MainNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "decks") {
        composable("decks") {
            DecksScreen(
                onCrear = { nav.navigate("create") },
                onJugar = { id -> nav.navigate("game/$id") },
                onHistorial = { nav.navigate("history") },
            )
        }
        composable("create") {
            CreateDeckScreen(onVolver = { nav.popBackStack() })
        }
        composable(
            route = "game/{mazoId}",
            arguments = listOf(navArgument("mazoId") { type = NavType.StringType })
        ) {
            GameScreen(onVolver = { nav.popBackStack() })
        }
        composable("history") {
            HistoryScreen(onVolver = { nav.popBackStack() })
        }
    }
}