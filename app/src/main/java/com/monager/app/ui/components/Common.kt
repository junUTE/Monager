package com.monager.app.ui.components

import androidx.compose.foundation.Canvas
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
    ) { MIconView(icon, if (selected) c.primary else fg, size = 20.dp) }
}

@Composable
fun AppIconButton(icon: MIcon, label: String, onClick: () -> Unit = {}, tint: Color = mc.text, raisedStyle: Boolean = false) {
    val base = Modifier.size(40.dp)
    if (raisedStyle) {
        RaisedBox(base.size(36.dp), radius = 12.dp, onClick = onClick) { MIconView(icon, tint) }
    } else {
        Box(base.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
            MIconView(icon, tint)
        }
    }
}

@Composable
fun ScreenHeader(title: String, subtitle: String? = null, onBack: (() -> Unit)? = null, right: @Composable () -> Unit = {}) {
    val c = mc
    Row(
        Modifier.fillMaxWidth().height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onBack != null) AppIconButton(MIcon.Back, "Quay lại", onBack, c.text)
            Column {
                Text(title, color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.sp)
            }
        }
        right()
    }
}

@Composable
fun SectionTitle(title: String, action: String? = null, color: Color = mc.text, actionColor: Color = mc.primary) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        if (action != null) Text(action, color = actionColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Inset segmented control (pressed-in track, raised active chip). */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val c = mc
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEachIndexed { i, label ->
            val on = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(38.dp)
                    .then(if (on) Modifier.raised(radius = 10.dp, depth = 2.dp) else Modifier)
                    .clickable { onSelect(i) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label, color = if (on) c.primary else c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
            }
        }
    }
}

/** Progress bar drawn in a single Canvas pass; [fraction] is read in the draw phase. */
@Composable
fun AppProgress(fraction: Float, modifier: Modifier = Modifier, danger: Boolean = false, thumb: Boolean = true, trackColor: Color = mc.surface2, fillColor: Color? = null) {
    val c = mc
    val fill = fillColor ?: if (danger) c.expense else c.primary
    Canvas(modifier.fillMaxWidth().height(16.dp)) { drawProgress(fraction, trackColor, fill, thumb) }
}

@Composable
fun AppCard(modifier: Modifier = Modifier, padding: Dp = 14.dp, content: @Composable () -> Unit) {
    Column(modifier.fillMaxWidth().raised().padding(padding)) { content() }
}
