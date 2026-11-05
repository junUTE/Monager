package com.monager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.Money
import com.monager.app.core.MoneyFormat
import com.monager.app.data.prefs.ThemeMode
import com.monager.app.ui.navigation.MonagerNavHost
import com.monager.app.ui.theme.MonagerTheme

class MainActivity : ComponentActivity() {
    override fun onStart() {
        super.onStart()
        (application as MonagerApp).container.refreshToday()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MonagerApp).container
        PerfTools.install(this, container)
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            // Display format for amounts (separators + currency mark); Money is Compose state, so screens update themselves.
            val moneyFormat by container.settings.moneyFormat.collectAsStateWithLifecycle(MoneyFormat())
            LaunchedEffect(moneyFormat) { Money.configure(moneyFormat) }
            val isDark = when (mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> systemDark }
            MonagerTheme(isDark) {
                MonagerNavHost(container, isDark)
            }
        }
    }
}
