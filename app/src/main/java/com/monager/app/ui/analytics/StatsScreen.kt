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
import com.monager.app.ui.components.drawFlowBars
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.raised
import com.monager.app.ui.components.rememberReveal
import com.monager.app.ui.components.tactile
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import com.monager.app.ui.components.drawDonut
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.SimpleSheet
import com.monager.app.ui.theme.mc

private val Kinds = listOf(PeriodKind.WEEK, PeriodKind.MONTH, PeriodKind.YEAR, PeriodKind.CUSTOM)
private val KindLabels = listOf("Week", "Month", "Year", "Custom")
// Five distinct slice colours + grey for "Other" (index MAX_SLICES - 1 in the ViewModel).
private val SlicePaletteLight = listOf(Color(0xFF1F6B52), Color(0xFFDC604E), Color(0xFF557A9B), Color(0xFFD29D3F), Color(0xFF8B6BB8), Color(0xFFAAB3AE))
private val SlicePaletteDark = listOf(Color(0xFF4FB98F), Color(0xFFF08A7A), Color(0xFF7FA6C9), Color(0xFFE6B960), Color(0xFFB39DDB), Color(0xFF8A9690))

@Composable
fun StatsScreen(
    vm: AnalyticsViewModel,
    onBack: (() -> Unit)? = null,
    onOpenCategory: (id: Long, from: Long, to: Long, period: String) -> Unit = { _, _, _, _ -> },
) {
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
                item(key = "summary") { SummaryCard(st) }
                item(key = "donut") { SpendingCard(st, palette) { id -> onOpenCategory(id, st.from, st.to, st.label) } }
                item(key = "flow") { FlowCard(st) }
            }
        }
        if (calendarSheet) CalendarSheet(vm, onClose = { calendarSheet = false })
        if (customSheet) {
            CustomRangeSheet(onApply = { f, t -> vm.setCustom(f, t); customSheet = false }, onClose = { customSheet = false })
        }
    }
}

/** Hero: net cash flow of the period with income / expense tiles and the expense change vs the previous period. */
@Composable
private fun SummaryCard(st: AnalyticsState) {
    val net = st.totalIncome - st.totalExpense
    Column(Modifier.fillMaxWidth().raised(radius = 20.dp, depth = 5.dp, brush = mc.heroBrush).padding(18.dp)) {
        Text("Net cash flow", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
        Text(
            (if (net > 0) "+" else if (net < 0) "−" else "") + Money.format(kotlin.math.abs(net)),
            color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HeroTile("Income", st.totalIncome, MIcon.ArrowDown, Color(0xFFB8E0C2), null, Modifier.weight(1f))
            HeroTile("Expense", st.totalExpense, MIcon.ArrowUp, Color(0xFFFFB4A8), st.changePct, Modifier.weight(1f))
        }
    }
}

@Composable
private fun HeroTile(label: String, amount: Long, icon: MIcon, accent: Color, changePct: Int?, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = .1f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(20.dp).clip(CircleShape).background(accent.copy(alpha = .22f)), contentAlignment = Alignment.Center) {
                MIconView(icon, accent, size = 13.dp)
            }
            Text(label, color = Color.White.copy(alpha = .85f), fontSize = 11.sp, modifier = Modifier.weight(1f))
            if (changePct != null) {
                // Spending less than before is good news.
                Text(
                    (if (changePct <= 0) "↓" else "↑") + "${kotlin.math.abs(changePct)}%",
                    color = if (changePct <= 0) Color(0xFFB8E0C2) else Color(0xFFFFB4A8), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(Money.format(amount), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(top = 6.dp))
    }
}

@Composable
private fun SpendingCard(st: AnalyticsState, palette: List<Color>, onOpenCategory: (Long) -> Unit) {
    val c = mc
    AppCard(radius = 18.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Spending by category", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(if (st.categories.isEmpty()) "Nothing spent yet" else "${st.categories.size} categories · tap one for details", color = c.muted, fontSize = 11.sp)
            }
        }
        Box(Modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) { Donut(st, palette) }
        if (st.categories.isEmpty()) {
            Text("No spending in this period.", color = c.muted, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        } else {
            val max = st.categories.first().amount.coerceAtLeast(1L)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                st.categories.forEach { cat ->
                    val key = palette[minOf(cat.colorIndex, palette.size - 1)]
                    // Rows grouped into the grey "Other" slice keep the grey key dot but use their own category colour.
                    val bar = if (cat.colorIndex >= palette.size - 1) cat.tone.fg else key
                    CategoryShareRow(cat, key, bar, cat.amount.toFloat() / max, st.label) { onOpenCategory(cat.id) }
                }
            }
        }
    }
}

/** Prominent, tappable category line: icon tile, name, share pill, amount, a share bar in its donut colour. */
@Composable
private fun CategoryShareRow(cat: CategoryShareUi, keyColor: Color, color: Color, frac: Float, revealKey: String, onClick: () -> Unit) {
    val c = mc
    Row(
        Modifier.fillMaxWidth().tactile(RowShape, scale = .98f, onClick = onClick).padding(horizontal = 6.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            CategoryIcon(cat.icon, cat.tone, size = 42.dp)
            // Colour key that ties the row to its donut slice.
            Box(Modifier.size(12.dp).clip(CircleShape).background(c.surface).padding(2.dp).clip(CircleShape).background(keyColor))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(cat.name, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(Money.format(cat.amount), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppProgress(frac, Modifier.weight(1f), thumb = false, fillColor = color, revealKey = revealKey)
                Text(
                    "${cat.percent}%", color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = .14f)).padding(horizontal = 6.dp, vertical = 2.dp),
                )
            }
        }
        MIconView(MIcon.ChevronRight, c.muted, size = 16.dp)
    }
}

private val RowShape = RoundedCornerShape(14.dp)

/** Canvas donut. Sweeps are precomputed in the ViewModel; the Stroke is remembered; the entrance sweep is read in draw. */
@Composable
private fun Donut(st: AnalyticsState, palette: List<Color>) {
    val c = mc
    val strokePx = with(LocalDensity.current) { 24.dp.toPx() }
    val style = remember(strokePx) { Stroke(strokePx) }
    val emptyRing = c.surface2
    val reveal = rememberReveal("${st.kind}-${st.label}", ready = !st.loading, durationMillis = 900)
    Box(Modifier.size(170.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) { drawDonut(st.slices, palette, emptyRing, strokePx, style, reveal.value) }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(compact(st.totalExpense), color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text("spent", color = c.muted, fontSize = 10.sp)
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

private val FlowAxisW = 38.dp

/** Cash flow: diverging income/expense bars; tap a bar to read its numbers in the header line. */
@Composable
private fun FlowCard(st: AnalyticsState) {
    val c = mc
    var selected by rememberSaveable(st.label) { mutableIntStateOf(-1) }
    val pick = st.bars.getOrNull(selected)
    AppCard(radius = 18.dp) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Cash flow", color = c.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(pick?.label ?: "Whole period · tap a bar", color = c.muted, fontSize = 11.sp)
            }
            Legend(c.income, "Income")
            Legend(c.expense, "Expense", Modifier.padding(start = 10.dp))
        }
        val inc = pick?.income ?: st.totalIncome
        val exp = pick?.expense ?: st.totalExpense
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FlowStat("In", "+" + Money.format(inc), c.income, Modifier.weight(1f))
            FlowStat("Out", "−" + Money.format(exp), c.expense, Modifier.weight(1f))
            val net = inc - exp
            FlowStat("Net", (if (net < 0) "−" else "+") + Money.format(kotlin.math.abs(net)), if (net < 0) c.expense else c.primary, Modifier.weight(1f))
        }
        if (st.peak <= 0L) {
            Text("No data in this period.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(vertical = 24.dp))
        } else {
            FlowChart(st, selected) { i -> selected = if (selected == i) -1 else i }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, color = mc.muted, fontSize = 10.sp)
    }
}

@Composable
private fun FlowStat(label: String, value: String, color: Color, modifier: Modifier) {
    val c = mc
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = .08f)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, color = c.muted, fontSize = 10.sp)
        Text(value, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FlowChart(st: AnalyticsState, selected: Int, onSelect: (Int) -> Unit) {
    val c = mc
    val bars = st.bars
    val density = LocalDensity.current
    val radius = remember(density) { CornerRadius(with(density) { 4.dp.toPx() }) }
    val reveal = rememberReveal("${st.kind}-${st.label}", ready = true, durationMillis = 800)
    val income = c.income
    val expense = c.expense
    val line = c.line
    val highlight = c.primarySoft
    val peak = remember(st.peak) { compact(st.peak) }
    // The tap detector lives across recompositions: read the latest bar count / callback, never stale captures.
    val barCount by rememberUpdatedState(bars.size)
    val select by rememberUpdatedState(onSelect)
    Column(Modifier.padding(top = 14.dp)) {
        Row(Modifier.fillMaxWidth().height(150.dp)) {
            Column(Modifier.width(FlowAxisW).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                Text("+$peak", color = c.muted, fontSize = 9.sp, maxLines = 1)
                Text("0", color = c.muted, fontSize = 9.sp, maxLines = 1)
                Text("−$peak", color = c.muted, fontSize = 9.sp, maxLines = 1)
            }
            Canvas(
                Modifier.weight(1f).fillMaxHeight().pointerInput(Unit) {
                    detectTapGestures { o ->
                        val n = barCount
                        if (n > 0) select((o.x / (size.width / n)).toInt().coerceIn(0, n - 1))
                    }
                },
            ) { drawFlowBars(bars, income, expense, line, highlight, selected, radius, reveal.value) }
        }
        Row(Modifier.fillMaxWidth().padding(start = FlowAxisW, top = 6.dp)) {
            bars.forEachIndexed { i, b ->
                Text(
                    b.label, color = if (i == selected) c.primary else c.muted, fontSize = 8.sp, maxLines = 1,
                    fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Normal,
                    modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                )
            }
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
