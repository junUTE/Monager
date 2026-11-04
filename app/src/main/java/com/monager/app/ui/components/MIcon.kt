package com.monager.app.ui.components

import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private fun circle(cx: Float, cy: Float, r: Float) =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

private fun rect(x: Float, y: Float, w: Float, h: Float) = "M$x ${y}h${w}v${h}h-${w}Z"

/**
 * Lucide-style stroke icons as in-code vectors: no PNG, no font, ~0 bytes of bitmap RAM.
 * Each ImageVector is built lazily once and cached.
 */
enum class MIcon(private val path: String) {
    Home("M3 11l9-8 9 8M5 10v10h14V10M9 20v-6h6v6"),
    Chart("M4 19V9M10 19V5M16 19v-7M22 19H2"),
    Wallet("M4 6h15a2 2 0 0 1 2 2v11H4a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h13M16 12h5v4h-5a2 2 0 0 1 0-4Z"),
    Users(circle(9f, 8f, 3f) + "M3 20v-2a6 6 0 0 1 12 0v2M16 5a3 3 0 0 1 0 6M17 14a5 5 0 0 1 4 5"),
    Plus("M12 5v14M5 12h14"),
    Search(circle(11f, 11f, 7f) + "M20 20l-4-4"),
    Calendar(rect(3f, 5f, 18f, 16f) + "M16 3v4M8 3v4M3 10h18"),
    ChevronDown("M7 10l5 5 5-5"),
    ArrowUp("M12 19V5M7 10l5-5 5 5"),
    ArrowDown("M12 5v14M7 14l5 5 5-5"),
    Transfer("M5 8h14l-3-3M19 16H5l3 3"),
    Utensils("M6 3v8M3 3v5c0 2 1 3 3 3s3-1 3-3V3M6 11v10M16 3v18M16 3c3 1 5 4 5 8h-5"),
    Car("M3 16l2-7h14l2 7v4h-3v-2H6v2H3ZM7 9l2-4h6l2 4M7 14h.01M17 14h.01"),
    ShoppingBag("M5 8h14l-1 13H6ZM9 10V6a3 3 0 0 1 6 0v4"),
    Receipt("M5 3v18l3-2 3 2 3-2 3 2 2-2V3l-3 2-3-2-3 2-3-2ZM9 9h6M9 13h6"),
    More(circle(5f, 12f, 1f) + circle(12f, 12f, 1f) + circle(19f, 12f, 1f)),
    Settings(
        circle(12f, 12f, 3f) +
            "M19.4 15a1.7 1.7 0 0 0 .34 1.88l.06.06-2.83 2.83-.06-.06A1.7 1.7 0 0 0 15 19.4a1.7 1.7 0 0 0-1 .6 1.7 1.7 0 0 0-.4 1v.1h-4v-.1a1.7 1.7 0 0 0-1.1-1.6 1.7 1.7 0 0 0-1.88.34l-.06.06-2.83-2.83.06-.06A1.7 1.7 0 0 0 4.6 15a1.7 1.7 0 0 0-.6-1 1.7 1.7 0 0 0-1-.4h-.1v-4H3A1.7 1.7 0 0 0 4.6 8.5a1.7 1.7 0 0 0-.34-1.88l-.06-.06 2.83-2.83.06.06A1.7 1.7 0 0 0 9 4.6a1.7 1.7 0 0 0 1-.6 1.7 1.7 0 0 0 .4-1v-.1h4V3A1.7 1.7 0 0 0 15.5 4.6a1.7 1.7 0 0 0 1.88-.34l.06-.06 2.83 2.83-.06.06A1.7 1.7 0 0 0 19.4 9c.1.4.3.8.6 1 .3.3.7.4 1 .4h.1v4H21a1.7 1.7 0 0 0-1.6.6Z",
    ),
    Back("M15 18l-6-6 6-6"),
    Sun(circle(12f, 12f, 4f) + "M12 2v2M12 20v2M4.93 4.93l1.42 1.42M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.42-1.42M17.66 6.34l1.41-1.41"),
    Moon("M21 12.8A9 9 0 1 1 11.2 3 7 7 0 0 0 21 12.8Z"),
    Plane("M22 2l-7 20-4-9-9-4ZM22 2L11 13"),
    Heart("M20.8 4.6a5.5 5.5 0 0 0-7.8 0L12 5.7l-1.1-1.1a5.5 5.5 0 0 0-7.8 7.8l1.1 1.1L12 21l7.8-7.5 1.1-1.1a5.5 5.5 0 0 0-.1-7.8Z"),
    Graduation("M2 10l10-5 10 5-10 5ZM6 12v5c3 2 9 2 12 0v-5M22 10v6"),
    House("M3 11l9-8 9 8M5 10v10h14V10"),
    Gift(rect(3f, 8f, 18f, 13f) + "M12 8v13M3 12h18M12 8H7.5A2.5 2.5 0 1 1 10 5.5ZM12 8h4.5A2.5 2.5 0 1 0 14 5.5Z"),
    Briefcase(rect(3f, 7f, 18f, 13f) + "M9 7V4h6v3M3 12h18M10 12v2h4v-2"),
    Edit("M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L8 18l-4 1 1-4Z"),
    Drag(circle(9f, 7f, .8f) + circle(15f, 7f, .8f) + circle(9f, 12f, .8f) + circle(15f, 12f, .8f) + circle(9f, 17f, .8f) + circle(15f, 17f, .8f)),
    ChevronRight("M9 18l6-6-6-6"),
    Check("M5 12l4 4L19 6"),
    Close("M18 6L6 18M6 6l12 12"),
    Delete("M3 6h18M8 6V4h8v2M19 6l-1 15H6L5 6M10 11v5M14 11v5"),
    Coffee("M4 7h13v7a5 5 0 0 1-5 5H9a5 5 0 0 1-5-5ZM17 9h2a3 3 0 0 1 0 6h-2M7 3v2M11 3v2"),
    Phone(rect(5f, 2f, 14f, 20f) + "M9 18h6"),
    Sparkles("M12 3l1.2 3.8L17 8l-3.8 1.2L12 13l-1.2-3.8L7 8l3.8-1.2ZM5 14l.8 2.2L8 17l-2.2.8L5 20l-.8-2.2L2 17l2.2-.8ZM19 14l.6 1.4L21 16l-1.4.6L19 18l-.6-1.4L17 16l1.4-.6Z");

    val vector: ImageVector by lazy(LazyThreadSafetyMode.NONE) {
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).addPath(
            pathData = addPathNodes(path),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        ).build()
    }
}

@Composable
fun MIconView(icon: MIcon, tint: Color, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    Icon(imageVector = icon.vector, contentDescription = null, tint = tint, modifier = modifier.size(size))
}
