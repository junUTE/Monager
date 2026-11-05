package com.monager.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.dp
import com.monager.app.ui.theme.mc
import kotlin.math.roundToLong

/**
 * Draggable thumb slider for a money limit (tap or drag anywhere on the track). Values snap to [step] and
 * never go below one step. One Canvas; the only per-drag work is Long math.
 */
@Composable
fun LimitSlider(value: Long, max: Long, step: Long, onChange: (Long) -> Unit, modifier: Modifier = Modifier) {
    val c = mc
    val latest by rememberUpdatedState(onChange)
    val frac = if (max <= 0) 0f else (value.toFloat() / max).coerceIn(0f, 1f)
    val track = c.surface2
    val fill = c.primary
    Canvas(
        modifier.fillMaxWidth().height(36.dp).pointerInput(max, step) {
            val r = 12.dp.toPx()
            fun emit(x: Float) {
                val f = ((x - r) / (size.width - 2 * r)).coerceIn(0f, 1f)
                latest(((f * max / step).roundToLong() * step).coerceIn(step, max))
            }
            awaitEachGesture {
                val down = awaitFirstDown()
                down.consume()
                emit(down.position.x)
                do {
                    val event = awaitPointerEvent()
                    event.changes.forEach { if (it.positionChanged()) { emit(it.position.x); it.consume() } }
                } while (event.changes.any { it.pressed })
            }
        },
    ) {
        val r = 12.dp.toPx()
        val h = 8.dp.toPx()
        val top = (size.height - h) / 2f
        val corner = CornerRadius(h / 2f)
        val x = r + (size.width - 2 * r) * frac
        drawRoundRect(track, Offset(r, top), Size(size.width - 2 * r, h), corner)
        drawRoundRect(fill, Offset(r, top), Size(x - r, h), corner)
        drawCircle(Color(0x33000000), r, Offset(x + 1.dp.toPx(), size.height / 2f + 1.5.dp.toPx()))
        drawCircle(Color.White, r, Offset(x, size.height / 2f))
        drawCircle(fill, 4.dp.toPx(), Offset(x, size.height / 2f))
    }
}
