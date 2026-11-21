package com.monager.app.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.monager.app.ui.theme.mc

/**
 * Cheap Neo-3D: soft offset "shadow" rect + 2-stop gradient + 1px highlight border.
 * No blur(), no elevation shadow -> near-zero GPU cost on Mali.
 * With [press], the shadow tucks in and a tint darkens the face while pressed; both are read in the draw phase.
 */
@Composable
fun Modifier.raised(
    radius: Dp = 16.dp,
    depth: Dp = 4.dp,
    brush: Brush? = null,
    press: PressState? = null,
    pressTint: Color = Color.Unspecified,
): Modifier {
    val c = mc
    val shape = remember(radius) { RoundedCornerShape(radius) }
    val fill = brush ?: c.cardBrush
    val shadow = c.shadow
    val tint = if (pressTint.isSpecified) pressTint else if (c.dark) Color.Black else c.primary
    return this
        .drawBehind {
            val p = press?.value?.coerceIn(0f, 1f) ?: 0f
            val d = depth.toPx() * (1f - .75f * p)
            val r = CornerRadius(radius.toPx())
            drawRoundRect(color = shadow, topLeft = Offset(d * .6f, d * 1.4f), size = size, cornerRadius = r)
            drawRoundRect(brush = fill, size = size, cornerRadius = r)
            if (p > 0f) drawRoundRect(color = tint, size = size, cornerRadius = r, alpha = .10f * p)
        }
        .border(1.dp, c.highlight, shape)
}

/**
 * Tactile button: raised surface that sinks, shrinks a little and darkens while pressed, then springs back.
 * The ripple is clipped to the same rounded shape. Press animation runs in the draw/layer phase only.
 */
@Composable
fun RaisedBox(
    modifier: Modifier = Modifier,
    radius: Dp = 16.dp,
    brush: Brush? = null,
    onClick: () -> Unit = {},
    contentAlignment: Alignment = Alignment.Center,
    pressScale: Float = .96f,
    pressTint: Color = Color.Unspecified,
    content: @Composable BoxScope.() -> Unit,
) {
    val press = rememberPress()
    val shape = remember(radius) { RoundedCornerShape(radius) }
    Box(
        modifier = modifier
            .pressScale(press, pressScale, sink = 2.dp)
            .raised(radius = radius, depth = 4.dp, brush = brush, press = press, pressTint = pressTint)
            .clip(shape)
            .clickable(interactionSource = press.source, indication = LocalIndication.current, onClick = onClick),
        contentAlignment = contentAlignment,
        content = content,
    )
}
