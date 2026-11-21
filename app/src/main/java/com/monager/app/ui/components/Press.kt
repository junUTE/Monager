package com.monager.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest

/**
 * Press feedback shared by every tappable surface. [value] goes 0 -> 1 while pressed and springs back with a small
 * overshoot on release. It is only ever read inside graphicsLayer / draw lambdas, so pressing never recomposes.
 */
@Stable
class PressState internal constructor(val source: MutableInteractionSource, private val anim: Animatable<Float, AnimationVector1D>) {
    val value: Float get() = anim.value
}

@Composable
fun rememberPress(source: MutableInteractionSource = remember { MutableInteractionSource() }): PressState {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(source) {
        source.interactions.collectLatest { i ->
            when (i) {
                is PressInteraction.Press -> anim.animateTo(1f, tween(90))
                is PressInteraction.Release, is PressInteraction.Cancel -> {
                    // A quick tap emits Press + Release together: still show a visible dip before springing back.
                    if (anim.value < .7f) anim.animateTo(1f, tween(60))
                    anim.animateTo(0f, spring(dampingRatio = .45f, stiffness = 650f))
                }
            }
        }
    }
    return remember(source) { PressState(source, anim) }
}

/**
 * Scale (and optional sink) driven by [press] in the layer phase. The shrink is capped at ~10dp in total, so a
 * full-width row dips as gently as a small key instead of visibly collapsing.
 */
fun Modifier.pressScale(press: PressState, scale: Float = .96f, sink: Dp = 0.dp): Modifier = graphicsLayer {
    val p = press.value
    val shrink = minOf(1f - scale, 10.dp.toPx() / size.width.coerceAtLeast(1f))
    val s = 1f - shrink * p
    scaleX = s
    scaleY = s
    translationY = sink.toPx() * p.coerceIn(0f, 1f)
}

/**
 * Tap target for flat rounded surfaces (rows, chips, text actions): soft scale + a ripple clipped to [shape],
 * so a rounded control never shows a square ripple.
 */
@Composable
fun Modifier.tactile(shape: Shape, enabled: Boolean = true, scale: Float = .97f, onClick: () -> Unit): Modifier {
    val press = rememberPress()
    return this.pressScale(press, scale).clip(shape).clickable(press.source, LocalIndication.current, enabled = enabled, onClick = onClick)
}

/**
 * Entrance progress for charts: animates 0 -> 1 the first time [ready] is true for [key] (a new period or tab replays it).
 * The "played" flag is saved, so scrolling a chart back into a LazyColumn or rotating does not replay it.
 * Read `.value` only inside the Canvas lambda (draw phase).
 */
@Composable
fun rememberReveal(key: Any? = Unit, ready: Boolean = true, durationMillis: Int = 750): Animatable<Float, AnimationVector1D> {
    var played by rememberSaveable(key) { mutableStateOf(false) }
    val anim = remember(key) { Animatable(if (played) 1f else 0f) }
    LaunchedEffect(key, ready) {
        if (ready && !played) {
            anim.animateTo(1f, tween(durationMillis, easing = FastOutSlowInEasing))
            played = true
        }
    }
    return anim
}
