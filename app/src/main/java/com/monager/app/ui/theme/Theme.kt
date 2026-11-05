package com.monager.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Static design tokens (from the Figma Make mockup). Two instances: light + dark. */
@Immutable
class MonagerColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val text: Color,
    val muted: Color,
    val line: Color,
    val primary: Color,
    val primarySoft: Color,
    val onPrimary: Color,
    val income: Color,
    val expense: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val shadow: Color,
    val highlight: Color,
    val heroBrush: Brush,
    val cardBrush: Brush,
)

private val HeroBrush = Brush.linearGradient(listOf(Color(0xFF2C5E4B), Color(0xFF143D30), Color(0xFF0E3025)))

val LightColors = MonagerColors(
    dark = false,
    bg = Color(0xFFF3F6F4),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFE3E9E5),
    text = Color(0xFF111827),
    muted = Color(0xFF4B5563),
    line = Color(0xFFE0E6E2),
    primary = Color(0xFF143D30),
    primarySoft = Color(0xFFE6EFEB),
    onPrimary = Color.White,
    income = Color(0xFF16A34A),
    expense = Color(0xFFDC2626),
    cardTop = Color(0xFFFFFFFF),
    cardBottom = Color(0xFFF2F5F2),
    shadow = Color(0x240F172A),
    highlight = Color(0xE6FFFFFF),
    heroBrush = HeroBrush,
    cardBrush = Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFF2F5F2))),
)

val DarkColors = MonagerColors(
    dark = true,
    bg = Color(0xFF0F1914),
    surface = Color(0xFF1B2620),
    surface2 = Color(0xFF111E18),
    text = Color(0xFFF9FAFB),
    muted = Color(0xFFC2CCC6),
    line = Color(0xFF34443B),
    primary = Color(0xFF84A98C),
    primarySoft = Color(0xFF263C33),
    onPrimary = Color(0xFF101713),
    income = Color(0xFF4ADE80),
    expense = Color(0xFFF87171),
    cardTop = Color(0xFF26342D),
    cardBottom = Color(0xFF17231D),
    shadow = Color(0x47000000),
    highlight = Color(0x1FFFFFFF),
    heroBrush = HeroBrush,
    cardBrush = Brush.verticalGradient(listOf(Color(0xFF26342D), Color(0xFF17231D))),
)

val LocalMonagerColors = staticCompositionLocalOf { LightColors }

@Composable
fun MonagerTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.primary, background = colors.bg, surface = colors.surface, onSurface = colors.text,
            secondaryContainer = colors.primarySoft, onSecondaryContainer = colors.primary,
            surfaceContainerHigh = colors.surface, surfaceContainerHighest = colors.surface2,
        )
    } else {
        lightColorScheme(
            primary = colors.primary, background = colors.bg, surface = colors.surface, onSurface = colors.text,
            secondaryContainer = colors.primarySoft, onSecondaryContainer = colors.primary,
            surfaceContainerHigh = colors.surface, surfaceContainerHighest = colors.surface2,
        )
    }
    CompositionLocalProvider(LocalMonagerColors provides colors) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}

val mc: MonagerColors
    @Composable get() = LocalMonagerColors.current
