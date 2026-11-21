package com.monager.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.monager.app.ui.analytics.BarUi
import com.monager.app.ui.analytics.SliceUi

/*
 * All chart drawing lives here as plain DrawScope functions so it can be unit-tested for allocations
 * (DrawAllocationTest). Rules: no objects created per call. Offset / Size / CornerRadius / Color are value
 * classes (packed longs), Stroke / Brush / Path / lists must be created by the caller with remember.
 */

/** Donut with a hairline gap between slices; [progress] (0..1) sweeps it in clockwise. */
internal fun DrawScope.drawDonut(slices: List<SliceUi>, palette: List<Color>, emptyRing: Color, strokePx: Float, style: Stroke, progress: Float = 1f) {
    val inset = strokePx / 2f
    val arcSize = Size(size.width - strokePx, size.height - strokePx)
    drawArc(emptyRing, 0f, 360f, false, Offset(inset, inset), arcSize, style = style)
    if (slices.isEmpty()) return
    val gap = if (slices.size > 1) 1.6f else 0f
    val end = 360f * progress.coerceIn(0f, 1f)
    var start = 0f
    for (i in slices.indices) {
        if (start >= end) break
        val sweep = (minOf(slices[i].sweep, end - start) - gap).coerceAtLeast(0f)
        if (sweep > 0f) drawArc(palette[minOf(i, palette.size - 1)], start - 90f + gap / 2f, sweep, false, Offset(inset, inset), arcSize, style = style)
        start += slices[i].sweep
    }
}

/**
 * Reports cash flow as a diverging chart: income grows up from the middle baseline, expense grows down, both on the
 * same scale (fractions precomputed in the ViewModel). [selected] gets a soft highlight column. [progress] grows bars.
 */
internal fun DrawScope.drawFlowBars(
    bars: List<BarUi>, income: Color, expense: Color, line: Color, highlight: Color, selected: Int, radius: CornerRadius, progress: Float = 1f,
) {
    if (bars.isEmpty()) return
    val groupW = size.width / bars.size
    val barW = (groupW * .52f).coerceIn(3.dp.toPx(), 18.dp.toPx())
    val mid = size.height / 2f
    val half = mid - 2.dp.toPx()
    val gap = 1.5.dp.toPx()
    val p = progress.coerceIn(0f, 1f)
    if (selected in bars.indices) {
        drawRoundRect(highlight, Offset(groupW * selected + 2.dp.toPx(), 0f), Size(groupW - 4.dp.toPx(), size.height), CornerRadius(8.dp.toPx()))
    }
    drawLine(line, Offset(0f, 0.5f), Offset(size.width, 0.5f), 1f)
    drawLine(line, Offset(0f, size.height - 0.5f), Offset(size.width, size.height - 0.5f), 1f)
    for (i in bars.indices) {
        val b = bars[i]
        val x = groupW * i + (groupW - barW) / 2f
        val hi = (half - gap) * b.incomeFrac * p
        val he = (half - gap) * b.expenseFrac * p
        if (hi > 0f) drawRoundRect(income, Offset(x, mid - gap - hi), Size(barW, hi.coerceAtLeast(2f)), radius)
        if (he > 0f) drawRoundRect(expense, Offset(x, mid + gap), Size(barW, he.coerceAtLeast(2f)), radius)
    }
    drawLine(line, Offset(0f, mid), Offset(size.width, mid), 1.dp.toPx())
}

/** Home: one thin bar, the red part is the share of spending in (spending + income). */
internal fun DrawScope.drawSplitBar(hasData: Boolean, expenseFrac: Float, track: Color, income: Color, expense: Color) {
    val r = CornerRadius(size.height / 2f)
    if (!hasData) {
        drawRoundRect(track, Offset.Zero, size, r)
    } else {
        drawRoundRect(income, Offset.Zero, size, r)
        drawRoundRect(expense, Offset.Zero, Size(size.width * expenseFrac, size.height), r)
    }
}

/**
 * Home trend: cumulative total per day of the month as an area line. [values] (0..1, one per day up to today) are
 * normalised in the ViewModel; [days] is the length of the month so the line stops at today. The Paths are
 * remembered by the caller and only reset here, so nothing is allocated per frame.
 */
internal fun DrawScope.drawTrendLine(
    values: List<Float>, days: Int, gridLines: Int, path: Path, area: Path,
    line: Color, fill: Brush, grid: Color, lineStroke: Stroke, dash: PathEffect, progress: Float = 1f,
) {
    val pad = 6.dp.toPx()
    val h = size.height - pad * 2
    // Evenly spaced round-number guide lines (labels are Text beside the canvas); the last one is the solid zero baseline.
    val lines = gridLines.coerceAtLeast(2)
    for (g in 0 until lines) {
        val y = pad + h * g / (lines - 1).toFloat()
        drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx(), pathEffect = if (g == lines - 1) null else dash)
    }
    if (values.isEmpty()) return
    val span = (days - 1).coerceAtLeast(1)
    val bottom = pad + h
    path.reset(); area.reset()
    var px = 0f
    var py = 0f
    for (i in values.indices) {
        val x = size.width * i / span
        val y = pad + h * (1f - values[i])
        if (i == 0) {
            path.moveTo(x, y); area.moveTo(x, bottom); area.lineTo(x, y)
        } else {
            // Horizontal tangents at every point: smooth but monotone, so a cumulative curve never overshoots.
            val dx = (x - px) / 2f
            path.cubicTo(px + dx, py, x - dx, y, x, y)
            area.cubicTo(px + dx, py, x - dx, y, x, y)
        }
        px = x; py = y
    }
    val lastX = size.width * (values.size - 1) / span
    area.lineTo(lastX, bottom); area.close()
    // Entrance: the curve is revealed left to right, then today's dot fades in.
    val p = progress.coerceIn(0f, 1f)
    clipRect(right = (lastX + lineStroke.width * 2f) * p) {
        drawPath(area, fill)
        drawPath(path, line, style = lineStroke)
    }
    val dot = ((p - .85f) / .15f).coerceIn(0f, 1f)
    if (dot > 0f) {
        val lastY = pad + h * (1f - values.last())
        drawCircle(Color.White, 5.dp.toPx(), Offset(lastX, lastY), alpha = dot)
        drawCircle(line, 5.dp.toPx(), Offset(lastX, lastY), alpha = dot, style = lineStroke)
    }
}

/** Home: previous vs current period, two bars on tall tracks. */
internal fun DrawScope.drawCompareBars(prevFrac: Float, curFrac: Float, hasPrev: Boolean, hasCur: Boolean, track: Color, fill: Color, progress: Float = 1f) {
    val barW = size.width * 0.22f
    val r = CornerRadius(8.dp.toPx())
    val x0 = size.width * 0.25f - barW / 2f
    val x1 = size.width * 0.75f - barW / 2f
    drawRoundRect(track, Offset(x0, 0f), Size(barW, size.height), r)
    drawRoundRect(track, Offset(x1, 0f), Size(barW, size.height), r)
    val p = progress.coerceIn(0f, 1f)
    val ph = (size.height * prevFrac).coerceAtLeast(if (hasPrev) 6.dp.toPx() else 0f) * p
    val ch = (size.height * curFrac).coerceAtLeast(if (hasCur) 6.dp.toPx() else 0f) * p
    drawRoundRect(fill.copy(alpha = .55f), Offset(x0, size.height - ph), Size(barW, ph), r)
    drawRoundRect(fill, Offset(x1, size.height - ch), Size(barW, ch), r)
}

/**
 * Budgets overview ring. [frac] is spent / limit and may exceed 1: the first lap fills in [arcColor], the overflow
 * runs a second lap in [overColor] (capped at 200%) with a shaded knob at its head so the lap reads on top.
 * [strokeW] equals the width of both strokes; [progress] animates the sweep.
 */
internal fun DrawScope.drawBudgetRing(
    frac: Float, strokeW: Float, trackStroke: Stroke, arcStroke: Stroke, trackColor: Color, arcColor: Color,
    overColor: Color = arcColor, progress: Float = 1f,
) {
    val sz = Size(size.width - strokeW, size.height - strokeW)
    val topLeft = Offset(strokeW / 2, strokeW / 2)
    drawArc(trackColor, 0f, 360f, false, topLeft, sz, style = trackStroke)
    val shown = frac.coerceIn(0f, 2f) * progress.coerceIn(0f, 1f)
    if (shown <= 0f) return
    drawArc(arcColor, -90f, 360f * minOf(shown, 1f), false, topLeft, sz, style = arcStroke)
    if (shown > 1f) {
        val lap = 360f * (shown - 1f)
        drawArc(overColor, -90f, lap, false, topLeft, sz, style = arcStroke)
        val a = Math.toRadians((lap - 90f).toDouble())
        val rx = sz.width / 2f
        val cx = size.width / 2f + rx * kotlin.math.cos(a).toFloat()
        val cy = size.height / 2f + rx * kotlin.math.sin(a).toFloat()
        drawCircle(Color(0x33000000), strokeW * .46f, Offset(cx + strokeW * .06f, cy + strokeW * .08f))
        drawCircle(overColor, strokeW / 2f, Offset(cx, cy))
    }
}

/**
 * Shared by [AppProgress]: rounded track, rounded fill and an optional thumb. [fraction] above 1 (over budget) fills
 * the whole bar: up to the limit tick in [fill], the overflow in [over]. [progress] animates the fill in.
 */
internal fun DrawScope.drawProgress(fraction: Float, track: Color, fill: Color, thumb: Boolean, progress: Float = 1f, over: Color = fill) {
    val h = 8.dp.toPx()
    val top = (size.height - h) / 2f
    val r = CornerRadius(h / 2f)
    drawRoundRect(track, Offset(0f, top), Size(size.width, h), r)
    val f = fraction.coerceAtLeast(0f) * progress.coerceIn(0f, 1f)
    if (f <= 1f) {
        val w = size.width * f
        if (w > 0f) drawRoundRect(fill, Offset(0f, top), Size(w.coerceAtLeast(h), h), r)
        if (thumb) {
            val tr = 7.dp.toPx()
            val cx = w.coerceIn(tr, size.width - tr)
            drawCircle(Color(0x33000000), tr, Offset(cx + 1.dp.toPx(), size.height / 2f + 1.5.dp.toPx()))
            drawCircle(Color.White, tr, Offset(cx, size.height / 2f))
        }
    } else {
        val limitX = size.width / f
        drawRoundRect(over, Offset(0f, top), Size(size.width, h), r)
        drawRoundRect(fill, Offset(0f, top), Size(limitX, h), r)
        val tick = 2.dp.toPx()
        drawRoundRect(Color.White, Offset(limitX - tick / 2f, top - 3.dp.toPx()), Size(tick, h + 6.dp.toPx()), CornerRadius(tick / 2f))
    }
}

/** [LimitSlider] body: track, filled part and the draggable thumb at [frac] (0..1). */
internal fun DrawScope.drawLimitSlider(frac: Float, track: Color, fill: Color) {
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
