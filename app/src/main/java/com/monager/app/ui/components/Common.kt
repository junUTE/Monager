package com.monager.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.ui.theme.mc

/** Category color tones from the mockup. Background = fg at 12% alpha (works in light + dark). */
@Immutable
enum class Tone(val fg: Color) {
    Mint(Color(0xFF168145)), Sage(Color(0xFF16A34A)), Blue(Color(0xFF2563EB)), Peach(Color(0xFFD95813)),
    Yellow(Color(0xFFA97003)), Purple(Color(0xFF7C3AED)), Gray(Color(0xFF64748B));

    companion object {
        private val byName = entries.associateBy { it.name }
        fun fromKey(key: String?): Tone = byName[key] ?: Gray
    }
}

@Composable
fun CategoryIcon(icon: MIcon, tone: Tone, size: Dp = 40.dp, round: Boolean = false, selected: Boolean = false) {
    val c = mc
    val shape: Shape = if (round) CircleShape else RoundedCornerShape(12.dp)
    val fg = if (c.dark) tone.fg.copy(alpha = .95f) else tone.fg
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(if (selected) c.primarySoft else tone.fg.copy(alpha = .13f)),
        contentAlignment = Alignment.Center,
    ) { EmojiIcon(icon, if (selected) c.primary else fg, size = 20.dp) }
}

@Composable
fun AppIconButton(icon: MIcon, label: String, onClick: () -> Unit = {}, tint: Color = mc.text, raisedStyle: Boolean = false) {
    val base = Modifier.size(40.dp)
    if (raisedStyle) {
        RaisedBox(base.size(36.dp), radius = 12.dp, onClick = onClick) { MIconView(icon, tint) }
    } else {
        Box(base.tactile(IconButtonShape, scale = .88f, onClick = onClick).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
            MIconView(icon, tint)
        }
    }
}

private val IconButtonShape = RoundedCornerShape(12.dp)
private val PillShape = RoundedCornerShape(20.dp)

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, right: @Composable () -> Unit = {}) {
    val c = mc
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) AppIconButton(MIcon.Back, "Back", onBack, c.text)
            Column {
                Text(title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.sp)
            }
        }
        right()
    }
}

/** Section heading; [onAction] turns [action] into a pill-shaped tap target (rounded ripple, press scale). */
@Composable
fun SectionTitle(title: String, action: String? = null, color: Color = mc.text, actionColor: Color = mc.primary, onAction: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (action != null) TextAction(action, actionColor, onAction)
    }
}

/** Small bold text button with a rounded ripple; static text when [onClick] is null. */
@Composable
fun TextAction(text: String, color: Color = mc.primary, onClick: (() -> Unit)?, modifier: Modifier = Modifier, fontSize: Int = 12) {
    val m = if (onClick != null) modifier.tactile(PillShape, scale = .94f, onClick = onClick).padding(horizontal = 10.dp, vertical = 5.dp) else modifier
    Text(text, color = color, fontSize = fontSize.sp, fontWeight = FontWeight.Bold, modifier = m)
}

/**
 * Inset segmented control: pressed-in track with one raised chip that slides to the selected option.
 * The chip is drawn in the track's draw phase from an animated index, so switching never relayouts the options.
 */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = mc
    val density = LocalDensity.current
    val hairline = remember(density) { Stroke(with(density) { 1.dp.toPx() }) }
    val pos by animateFloatAsState(selected.toFloat(), spring(dampingRatio = .8f, stiffness = 500f), label = "tab")
    val count = options.size
    val fill = c.cardBrush
    val shadow = c.shadow
    val highlight = c.highlight
    Row(
        modifier.fillMaxWidth().clip(TrackShape).background(c.surface2)
            .drawBehind {
                if (count == 0 || selected !in 0 until count) return@drawBehind
                val pad = 4.dp.toPx()
                val gap = 4.dp.toPx()
                val w = (size.width - pad * 2 - gap * (count - 1)) / count
                val h = size.height - pad * 2
                val x = pad + pos * (w + gap)
                val r = CornerRadius(10.dp.toPx())
                val d = 2.dp.toPx()
                drawRoundRect(shadow, Offset(x + d * .6f, pad + d * 1.4f), Size(w, h), r)
                drawRoundRect(fill, Offset(x, pad), Size(w, h), r)
                drawRoundRect(highlight, Offset(x, pad), Size(w, h), r, style = hairline)
            }
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier.weight(1f).height(38.dp).tactile(ChipShape, scale = .95f) { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) c.primary else c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

private val TrackShape = RoundedCornerShape(14.dp)
private val ChipShape = RoundedCornerShape(10.dp)

/**
 * Progress bar drawn in a single Canvas pass; [fraction] and the entrance animation are read in the draw phase.
 * [fraction] may exceed 1: the bar then fills completely, a tick marks the limit and the overflow is drawn in [overColor].
 */
@Composable
fun AppProgress(
    fraction: Float, modifier: Modifier = Modifier, danger: Boolean = false, thumb: Boolean = true,
    trackColor: Color = mc.surface2, fillColor: Color? = null, overColor: Color = mc.expense, animate: Boolean = true, revealKey: Any? = Unit,
) {
    val c = mc
    val fill = fillColor ?: if (danger) c.expense else c.primary
    val reveal = rememberReveal(revealKey)
    Canvas(modifier.fillMaxWidth().height(16.dp)) {
        drawProgress(fraction, trackColor, fill, thumb, if (animate) reveal.value else 1f, overColor)
    }
}

/** Raised card; with [onClick] the whole card presses in (scale) and shows a ripple clipped to its corners. */
@Composable
fun AppCard(modifier: Modifier = Modifier, padding: Dp = 14.dp, radius: Dp = 16.dp, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    if (onClick == null) {
        Column(modifier.fillMaxWidth().raised(radius = radius).padding(padding)) { content() }
    } else {
        val press = rememberPress()
        val shape = remember(radius) { RoundedCornerShape(radius) }
        Column(
            modifier.fillMaxWidth().pressScale(press, .98f).raised(radius = radius, press = press).clip(shape)
                .clickable(press.source, LocalIndication.current, onClick = onClick).padding(padding),
        ) { content() }
    }
}
