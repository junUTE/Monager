package com.monager.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Emerald = Color(0xFF0F7B5F)
val Cream = Color(0xFFF6EFE0)

private val Scheme = lightColorScheme(primary = Emerald, background = Cream, surface = Cream)

@Composable
fun MonagerTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
