package com.monager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.monager.app.ui.theme.mc

/**
 * Cheap Neo-3D: soft offset "shadow" rect + 2-stop gradient + 1px highlight border.
 * No blur(), no elevation shadow -> near-zero GPU cost on Mali.
 */
@Composable
fun Modifier.raised(radius: Dp = 16.dp, depth: Dp = 4.dp, brush: Brush? = null): Modifier {
    val c = mc
    val shape = remember(radius) { RoundedCornerShape(radius) }
    val fill = brush ?: c.cardBrush
    return this
        .drawBehind {
            val d = depth.toPx()
            drawRoundRect(
                color = c.shadow,
                topLeft = Offset(d * .6f, d * 1.4f),
                size = size,
                cornerRadius = CornerRadius(radius.toPx()),
            )
        }
        .background(fill, shape)
        .border(1.dp, c.highlight, shape)
}

/** Tactile button: raised surface that sinks 2dp while pressed (draw-phase only, no recomposition). */
@Composable
fun RaisedBox(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    brush: Brush? = null,
    onClick: () -> Unit = {},
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .graphicsLayer { translationY = if (pressed) 2.dp.toPx() else 0f }
            .raised(radius = radius, depth = 4.dp, brush = brush)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = contentAlignment,
        content = content,
    )
}
