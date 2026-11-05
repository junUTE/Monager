package com.monager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.data.prefs.ThemeMode
import com.monager.app.ui.navigation.MonagerNavHost
import com.monager.app.ui.theme.MonagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MonagerApp).container
        PerfTools.install(this, container)
        setContent {
            val mode by container.settings.themeMode.collectAsStateWithLifecycle(ThemeMode.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val isDark = when (mode) { ThemeMode.DARK -> true; ThemeMode.LIGHT -> false; ThemeMode.SYSTEM -> systemDark }
            MonagerTheme(isDark) {
                MonagerNavHost(container, isDark)
            }
        }
    }
}
