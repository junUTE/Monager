package com.monager.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.monager.app.di.AppContainer
import com.monager.app.ui.home.HomeScreen

object Routes {
    const val HOME = "home"
    const val ANALYTICS = "analytics"
    const val BUDGETS = "budgets"
    const val DEBTS = "debts"
    const val SETTINGS = "settings"
}

@Composable
fun MonagerNavHost(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen() }
        // TODO: analytics, budgets, debts, settings (one package each under ui/)
    }
}
