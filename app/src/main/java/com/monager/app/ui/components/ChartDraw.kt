package com.monager.app.ui.components

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.monager.app.ui.analytics.BarUi
import com.monager.app.ui.analytics.SliceUi

/*
 * All chart drawing lives here as plain DrawScope functions so it can be unit-tested for allocations
 * (DrawAllocationTest). Rules: no objects created per call. Offset / Size / CornerRadius / Color are value
 * classes (packed longs), Stroke / Brush / Path / lists must be created by the caller with remember.
 */

internal fun DrawScope.drawDonut(slices: List<SliceUi>, palette: List<Color>, emptyRing: Color, strokePx: Float, style: Stroke) {
    val inset = strokePx / 2f
    val arcSize = Size(size.width - strokePx, size.height - strokePx)
    if (slices.isEmpty()) {
        drawArc(emptyRing, 0f, 360f, false, Offset(inset, inset), arcSize, style = style)
        return
    }
    var start = -90f
    for (i in slices.indices) {
        val sweep = slices[i].sweep
        drawArc(palette[i % palette.size], start, sweep, false, Offset(inset, inset), arcSize, style = style)
        start += sweep
    }
}

internal fun DrawScope.drawCashFlowBars(bars: List<BarUi>, income: Color, expense: Color, line: Color, gap: Float, radius: CornerRadius) {
    val groupW = size.width / bars.size
    val barW = ((groupW - gap * 3) / 2f).coerceIn(2f, 14.dp.toPx())
    for (g in 1..3) {
        val y = size.height * g / 4f
        drawLine(line, Offset(0f, y), Offset(size.width, y), 1f)
    }
    for (i in bars.indices) {
        val b = bars[i]
        val x0 = groupW * i + (groupW - (barW * 2 + gap)) / 2f
        val hi = size.height * b.incomeFrac
        val he = size.height * b.expenseFrac
        if (hi > 0f) drawRoundRect(income, Offset(x0, size.height - hi), Size(barW, hi), radius)
        if (he > 0f) drawRoundRect(expense, Offset(x0 + barW + gap, size.height - he), Size(barW, he), radius)
    }
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

/** Home: expense per day of the month, values already normalised to 0..1 in the ViewModel. */
internal fun DrawScope.drawDailyBars(values: List<Float>, expense: Color, track: Color) {
    val slot = size.width / values.size
    val barW = slot * 0.6f
    val r = CornerRadius(barW / 2f)
    for (i in values.indices) {
        val v = values[i]
        val h = (size.height * v).coerceAtLeast(if (v > 0f) 4.dp.toPx() else 2.dp.toPx())
        drawRoundRect(if (v > 0f) expense else track, Offset(i * slot + (slot - barW) / 2f, size.height - h), Size(barW, h), r)
    }
}

/** Home: previous vs current period, two bars on tall tracks. */
internal fun DrawScope.drawCompareBars(prevFrac: Float, curFrac: Float, hasPrev: Boolean, hasCur: Boolean, track: Color, fill: Color) {
    val barW = size.width * 0.22f
    val r = CornerRadius(8.dp.toPx())
    val x0 = size.width * 0.25f - barW / 2f
    val x1 = size.width * 0.75f - barW / 2f
    drawRoundRect(track, Offset(x0, 0f), Size(barW, size.height), r)
    drawRoundRect(track, Offset(x1, 0f), Size(barW, size.height), r)
    val ph = (size.height * prevFrac).coerceAtLeast(if (hasPrev) 6.dp.toPx() else 0f)
    val ch = (size.height * curFrac).coerceAtLeast(if (hasCur) 6.dp.toPx() else 0f)
    drawRoundRect(fill.copy(alpha = .55f), Offset(x0, size.height - ph), Size(barW, ph), r)
    drawRoundRect(fill, Offset(x1, size.height - ch), Size(barW, ch), r)
}

/** Budgets overview ring. [strokeW] equals the width of both strokes. */
internal fun DrawScope.drawBudgetRing(frac: Float, strokeW: Float, trackStroke: Stroke, arcStroke: Stroke, trackColor: Color, arcColor: Color) {
    val sz = Size(size.width - strokeW, size.height - strokeW)
    val topLeft = Offset(strokeW / 2, strokeW / 2)
    drawArc(trackColor, 0f, 360f, false, topLeft, sz, style = trackStroke)
    drawArc(arcColor, -90f, 360f * frac, false, topLeft, sz, style = arcStroke)
}

/** Shared by [AppProgress]: rounded track, rounded fill and an optional thumb. */
internal fun DrawScope.drawProgress(fraction: Float, track: Color, fill: Color, thumb: Boolean) {
    val h = 8.dp.toPx()
    val top = (size.height - h) / 2f
    val r = CornerRadius(h / 2f)
    drawRoundRect(track, Offset(0f, top), Size(size.width, h), r)
    val w = size.width * fraction.coerceIn(0f, 1f)
    drawRoundRect(fill, Offset(0f, top), Size(w, h), r)
    if (thumb) {
        val tr = 7.dp.toPx()
        val cx = w.coerceIn(tr, size.width - tr)
        drawCircle(Color(0x33000000), tr, Offset(cx + 1.dp.toPx(), size.height / 2f + 1.5.dp.toPx()))
        drawCircle(Color.White, tr, Offset(cx, size.height / 2f))
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
