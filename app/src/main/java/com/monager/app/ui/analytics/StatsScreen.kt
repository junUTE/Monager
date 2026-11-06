package com.monager.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.DebtMath
import com.monager.app.core.Money
import com.monager.app.core.PeriodKind
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.DateField
import com.monager.app.ui.components.drawCashFlowBars
import com.monager.app.ui.components.drawDonut
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.SimpleSheet
import com.monager.app.ui.theme.mc

private val Kinds = listOf(PeriodKind.WEEK, PeriodKind.MONTH, PeriodKind.YEAR, PeriodKind.CUSTOM)
private val KindLabels = listOf("Week", "Month", "Year", "Custom")
private val SlicePaletteLight = listOf(Color(0xFF143D30), Color(0xFFDC604E), Color(0xFF557A9B), Color(0xFFD29D3F), Color(0xFFAAB3AE))
private val SlicePaletteDark = listOf(Color(0xFF4FB98F), Color(0xFFF08A7A), Color(0xFF7FA6C9), Color(0xFFE6B960), Color(0xFF8A9690))

@Composable
fun StatsScreen(vm: AnalyticsViewModel, onBack: (() -> Unit)? = null) {
    val c = mc
    val st by vm.state.collectAsStateWithLifecycle()
    var customSheet by rememberSaveable { mutableStateOf(false) }
    var calendarSheet by rememberSaveable { mutableStateOf(false) }
    val palette = if (c.dark) SlicePaletteDark else SlicePaletteLight

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "h") {
                ScreenHeader("Reports", "Track your cash flow", onBack = onBack, right = { AppIconButton(MIcon.Calendar, "Calendar view", { calendarSheet = true }, c.primary) })
            }
            item(key = "seg") {
                SegmentedTabs(KindLabels, Kinds.indexOf(st.kind).coerceAtLeast(0), { i ->
                    if (Kinds[i] == PeriodKind.CUSTOM) customSheet = true else vm.setKind(Kinds[i])
                })
            }
            item(key = "nav") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    AppIconButton(MIcon.Back, "Previous period", { vm.shift(-1) }, if (st.kind == PeriodKind.CUSTOM) c.muted.copy(alpha = .3f) else c.text)
                    Text(st.label, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Box(Modifier.alpha(if (st.canGoNext) 1f else .3f)) { AppIconButton(MIcon.ChevronRight, "Next period", { if (st.canGoNext) vm.shift(1) }, c.text) }
                }
            }
            if (st.loading) {
                item(key = "loading") { Text("Loading…", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(24.dp)) }
            } else {
                item(key = "donut") { SpendingCard(st, palette) }
                item(key = "flow") { FlowCard(st) }
            }
        }
        if (calendarSheet) CalendarSheet(vm, onClose = { calendarSheet = false })
        if (customSheet) {
            CustomRangeSheet(onApply = { f, t -> vm.setCustom(f, t); customSheet = false }, onClose = { customSheet = false })
        }
    }
}

@Composable
private fun SpendingCard(st: AnalyticsState, palette: List<Color>) {
    val c = mc
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("Total spending", color = c.muted, fontSize = 10.sp)
                Text(Money.format(st.totalExpense), color = c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
            }
            st.changePct?.let { pct ->
                val good = pct <= 0 // spending less than before is good news
                val tone = if (good) Color(0xFF166534) else Color(0xFF991B1B)
                Text(
                    (if (pct <= 0) "↓ " else "↑ ") + "${kotlin.math.abs(pct)}%", color = tone, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (good) Color(0xFFE7F5EA) else Color(0xFFFDE8E6)).padding(horizontal = 8.dp, vertical = 5.dp),
                )
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) { Donut(st, palette) }
        if (st.slices.isEmpty()) {
            Text("No spending in this period.", color = c.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                st.slices.forEachIndexed { i, s ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(palette[i % palette.size]))
                        Text(s.name, color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                        Text("${s.percent}%", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(end = 12.dp))
                        Text(Money.format(s.amount), color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/** Canvas donut. Sweeps are precomputed in the ViewModel; the Stroke is remembered, so drawing allocates nothing new. */
@Composable
private fun Donut(st: AnalyticsState, palette: List<Color>) {
    val c = mc
    val strokePx = with(LocalDensity.current) { 21.dp.toPx() }
    val style = remember(strokePx) { Stroke(strokePx) }
    val emptyRing = c.surface2
    Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawDonut(st.slices, palette, emptyRing, strokePx, style) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(compact(st.totalExpense), color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(st.label, color = c.muted, fontSize = 8.sp, maxLines = 1, modifier = Modifier.padding(horizontal = 18.dp), textAlign = TextAlign.Center)
        }
    }
}

/** 8,320,000 -> "8.32M", 1,500 -> "1.5k": short enough for the donut hole. */
private fun compact(v: Long): String = when {
    v >= 1_000_000_000 -> "%.1fB".format(v / 1e9)
    v >= 1_000_000 -> "%.2fM".format(v / 1e6).replace(".00", "")
    v >= 1_000 -> "%.1fk".format(v / 1e3).replace(".0", "")
    else -> v.toString()
}

@Composable
private fun FlowCard(st: AnalyticsState) {
    val c = mc
    AppCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("Cash flow", color = c.muted, fontSize = 10.sp)
                Text("Income & expenses", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(6.dp).background(c.income, RoundedCornerShape(2.dp)))
                Text("Thu", color = c.muted, fontSize = 9.sp)
                Box(Modifier.size(6.dp).background(c.expense, RoundedCornerShape(2.dp)))
                Text("Chi", color = c.muted, fontSize = 9.sp)
            }
        }
        Text("Income ${Money.format(st.totalIncome)}  ·  Expense ${Money.format(st.totalExpense)}", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        if (st.bars.all { it.income == 0L && it.expense == 0L }) {
            Text("No data in this period.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            CashFlowBars(st.bars, Modifier.padding(top = 12.dp))
        }
    }
}

@Composable
private fun CashFlowBars(bars: List<BarUi>, modifier: Modifier) {
    val c = mc
    val density = LocalDensity.current
    val gap = with(density) { 3.dp.toPx() }
    val radius = remember(gap) { CornerRadius(with(density) { 4.dp.toPx() }) }
    Column(modifier) {
        val income = c.income
        val expense = c.expense
        val line = c.line
        Canvas(Modifier.fillMaxWidth().height(110.dp)) { drawCashFlowBars(bars, income, expense, line, gap, radius) }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.forEach { Text(it.label, color = c.muted, fontSize = 8.sp, maxLines = 1, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
        }
    }
}

private val WeekDays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val MonthFmt = java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy", java.util.Locale.ENGLISH)

/** Month grid; each day cell shows that day's income (+) and expense (-). Data is SQL-aggregated in the ViewModel. */
@Composable
private fun CalendarSheet(vm: AnalyticsViewModel, onClose: () -> Unit) {
    val c = mc
    val today = remember { java.time.LocalDate.now() }
    var ym by rememberSaveable(stateSaver = androidx.compose.runtime.saveable.Saver<java.time.YearMonth, String>({ it.toString() }, { java.time.YearMonth.parse(it) })) {
        mutableStateOf(java.time.YearMonth.from(today))
    }
    val days by remember(ym) { vm.monthDays(ym) }.collectAsStateWithLifecycle(emptyMap())
    val lead = ym.atDay(1).dayOfWeek.value - 1 // Monday-first
    val rows = (lead + ym.lengthOfMonth() + 6) / 7
    val totalIn = days.values.sumOf { it.income }
    val totalOut = days.values.sumOf { it.expense }
    SimpleSheet("Calendar", "Daily income and expenses", onClose) {
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            AppIconButton(MIcon.Back, "Previous month", { ym = ym.minusMonths(1) }, c.text)
            Text(ym.format(MonthFmt), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            AppIconButton(MIcon.ChevronRight, "Next month", { ym = ym.plusMonths(1) }, c.text)
        }
        Text("Income ${Money.format(totalIn)}  ·  Expense ${Money.format(totalOut)}", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(vertical = 6.dp))
        Row(Modifier.fillMaxWidth()) {
            WeekDays.forEach { Text(it, color = c.muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Center) }
        }
        for (r in 0 until rows) {
            Row(Modifier.fillMaxWidth().padding(top = 3.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                for (col in 0 until 7) {
                    val day = r * 7 + col - lead + 1
                    if (day < 1 || day > ym.lengthOfMonth()) {
                        Box(Modifier.weight(1f).height(52.dp))
                    } else {
                        val cell = days[day]
                        val isToday = ym.atDay(day) == today
                        Column(
                            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(8.dp))
                                .background(if (isToday) c.primary.copy(alpha = .18f) else c.surface2),
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text("$day", color = if (isToday) c.primary else c.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            if (cell != null && cell.income > 0) Text("+" + compact(cell.income), color = c.income, fontSize = 8.sp, maxLines = 1)
                            if (cell != null && cell.expense > 0) Text("-" + compact(cell.expense), color = c.expense, fontSize = 8.sp, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomRangeSheet(onApply: (java.time.LocalDate, java.time.LocalDate) -> Unit, onClose: () -> Unit) {
    val c = mc
    val today = java.time.LocalDate.now()
    var from by rememberSaveable { mutableStateOf(DebtMath.formatDate(today.minusDays(29))) }
    var to by rememberSaveable { mutableStateOf(DebtMath.formatDate(today)) }
    val f = DebtMath.parseDate(from)
    val t = DebtMath.parseDate(to)
    val ok = f != null && t != null && !f.isAfter(t)
    SimpleSheet("Date range", "Choose a start and end date", onClose) {
        Text("From", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        DateField(from, { from = it }, "From")
        Text("To", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        DateField(to, { to = it }, "To")
        if (f != null && t != null && f.isAfter(t)) Text("Start date must be before end date", color = c.expense, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        RaisedBox(
            Modifier.fillMaxWidth().padding(top = 14.dp).height(44.dp).alpha(if (ok) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
            onClick = { if (f != null && t != null) onApply(f, t) },
        ) { Text("Apply", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}
