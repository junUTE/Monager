package com.monager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.monager.app.ui.navigation.MonagerNavHost
import com.monager.app.ui.theme.MonagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MonagerApp).container
        setContent {
            // TODO(M8): persist theme choice in DataStore (Light / Dark / System).
            var dark by rememberSaveable { mutableStateOf<Boolean?>(null) }
            val isDark = dark ?: isSystemInDarkTheme()
            MonagerTheme(isDark) {
                MonagerNavHost(container, isDark, onToggleDark = { dark = !isDark })
            }
        }
    }
}
