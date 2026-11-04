package com.monager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.monager.app.ui.navigation.MonagerNavHost
import com.monager.app.ui.theme.MonagerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as MonagerApp).container
        setContent { MonagerTheme { MonagerNavHost(container) } }
    }
}
