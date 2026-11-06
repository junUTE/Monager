package com.monager.app.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.launch
import java.time.LocalDate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.R
import com.monager.app.core.Money
import com.monager.app.ui.HomeState
import com.monager.app.ui.PeriodSpend
import com.monager.app.ui.TopSpend
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.TransactionRow
import com.monager.app.ui.components.drawCompareBars
import com.monager.app.ui.components.drawTrendLine
import com.monager.app.ui.components.raised
import com.monager.app.ui.theme.mc

private val Periods = listOf("Week", "Month")

@Composable
fun HomeScreen(
    state: HomeState,
    hideBalance: Boolean,
    onToggleHide: () -> Unit,
    onSeeReports: () -> Unit,
    onSeeTransactions: () -> Unit,
    onEditTx: (Long) -> Unit,
) {
    val c = mc
    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "balance", contentType = "balance") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Image(
                        painterResource(R.drawable.monager_mark), contentDescription = "Monager",
                        modifier = Modifier.padding(end = 12.dp).size(44.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (hideBalance) "••••••••" else Money.format(state.totalBalance),
                            color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("Total balance", color = c.muted, fontSize = 12.sp)
                    }
                    AppIconButton(if (hideBalance) MIcon.EyeOff else MIcon.Eye, "Hide/show balance", onToggleHide, c.text)
                }
            }

            item(key = "report_title", contentType = "title") { Section("This month's report", "View reports", onSeeReports) }
            item(key = "report", contentType = "report") { ReportCard(state) }

            item(key = "top_title", contentType = "title") { Section("Top spending", "View details", onSeeReports) }
            item(key = "top", contentType = "top") { TopSpendingCard(state) }

            item(key = "recent_title", contentType = "title") { Section("Recent transactions", "See all", onSeeTransactions) }
            item(key = "recent", contentType = "recent") {
                Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 4.dp).padding(vertical = 4.dp)) {
                    if (state.loaded && state.recent.isEmpty()) {
                        Text("No transactions yet. Tap + to add your first one.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    }
                    state.recent.forEach { TransactionRow(it, showDate = true, onClick = { onEditTx(it.id) }) }
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, action: String, onClick: () -> Unit) {
    val c = mc
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Text(title, color = c.muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        Text(action, color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onClick).padding(vertical = 4.dp))
    }
}

/** One card, two swipeable pages: month trend (spent / income tabs) and week/month spending comparison. */
@Composable
private fun ReportCard(state: HomeState) {
    val c = mc
    val pager = rememberPagerState { 2 }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 4.dp).padding(horizontal = 16.dp, vertical = 14.dp)) {
        HorizontalPager(pager, Modifier.fillMaxWidth().height(244.dp), key = { it }) { page ->
            if (page == 0) TrendPage(state) else ComparePage(state)
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "‹", color = c.primary.copy(alpha = if (pager.currentPage > 0) 1f else .3f), fontSize = 26.sp, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable { scope.launch { pager.animateScrollToPage(0) } },
            )
            Text(if (pager.currentPage == 0) "This month's trend" else "Spending comparison", color = c.primary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.weight(4f))
            Text(
                "›", color = c.primary.copy(alpha = if (pager.currentPage < 1) 1f else .3f), fontSize = 26.sp, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f).clickable { scope.launch { pager.animateScrollToPage(1) } },
            )
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.Center) {
            repeat(2) { i ->
                Box(Modifier.padding(horizontal = 3.dp).size(6.dp).clip(CircleShape).background(if (i == pager.currentPage) c.primary else c.muted.copy(alpha = .35f)))
            }
        }
    }
}

private val AxisW = 36.dp

@Composable
private fun TrendPage(state: HomeState) {
    val c = mc
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val trend = if (tab == 0) state.trendExpense else state.trendIncome
    val values = trend.values
    val color = if (tab == 0) c.expense else c.income
    val density = LocalDensity.current
    val lineStroke = remember(density) { Stroke(with(density) { 2.5.dp.toPx() }, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val dash = remember { PathEffect.dashPathEffect(floatArrayOf(14f, 14f)) }
    val path = remember { Path() }
    val area = remember { Path() }
    val month = remember { LocalDate.now().monthValue }
    val grid = c.muted.copy(alpha = .3f)
    val chartH = 150.dp
    val fill = remember(color, density) {
        Brush.verticalGradient(listOf(color.copy(alpha = .30f), color.copy(alpha = .02f)), startY = 0f, endY = with(density) { chartH.toPx() })
    }
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            TrendTab("Total spent", state.month.current, c.expense, tab == 0, Modifier.weight(1f)) { tab = 0 }
            TrendTab("Total income", state.monthIncome, c.income, tab == 1, Modifier.weight(1f)) { tab = 1 }
        }
        if (values.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 10.dp).height(chartH), contentAlignment = Alignment.Center) {
                Text("Add transactions to see reports", color = c.muted, fontSize = 13.sp)
            }
        } else {
            val days = state.monthDays
            val gridLines = trend.labels.size
            Row(Modifier.fillMaxWidth().padding(top = 10.dp).height(chartH)) {
                // Round-number labels (e.g. 300k / 200k / 100k / 0), one per guide line, centred on it.
                Column(Modifier.width(AxisW).fillMaxHeight().padding(vertical = 6.dp), verticalArrangement = Arrangement.SpaceBetween) {
                    trend.labels.forEach { l ->
                        Box(Modifier.fillMaxWidth().height(0.dp), contentAlignment = Alignment.CenterEnd) {
                            Text(l, color = c.muted, fontSize = 9.sp, maxLines = 1, modifier = Modifier.wrapContentHeight(unbounded = true).padding(end = 6.dp))
                        }
                    }
                }
                Canvas(Modifier.weight(1f).fillMaxHeight()) { drawTrendLine(values, days, gridLines, path, area, color, fill, grid, lineStroke, dash) }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp, start = if (values.isEmpty()) 0.dp else AxisW), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("01/%02d".format(month), color = c.muted, fontSize = 10.sp)
            Text("${state.monthDays}/%02d".format(month), color = c.muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun TrendTab(label: String, amount: Long, color: Color, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = mc
    Column(modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = if (selected) c.text else c.muted, fontSize = 12.sp)
        Text(Money.format(amount), color = color, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Box(Modifier.padding(top = 6.dp).fillMaxWidth().height(2.dp).background(if (selected) color else c.surface2))
    }
}

@Composable
private fun ComparePage(state: HomeState) {
    val c = mc
    var period by rememberSaveable { mutableIntStateOf(1) }
    val p = if (period == 0) state.week else state.month
    val labels = if (period == 0) "Last week" to "This week" else "Last month" to "This month"
    Column(Modifier.fillMaxWidth()) {
        SegmentedTabs(Periods, period, { period = it })
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(Money.format(p.current), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            ChangeBadge(p)
        }
        Text(if (period == 0) "Spent this week" else "Spent this month", color = c.muted, fontSize = 12.sp)
        val max = maxOf(p.current, p.previous)
        val prevFrac = if (max <= 0) 0f else p.previous.toFloat() / max
        val curFrac = if (max <= 0) 0f else p.current.toFloat() / max
        val barColor = c.surface2
        val fill = c.primary
        Canvas(Modifier.fillMaxWidth().padding(top = 10.dp).height(90.dp)) { drawCompareBars(prevFrac, curFrac, p.previous > 0, p.current > 0, barColor, fill) }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            Text(labels.first, color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            Text(labels.second, color = c.muted, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun ChangeBadge(p: PeriodSpend) {
    val c = mc
    val (text, color) = when {
        p.previous <= 0 && p.current <= 0 -> "0%" to c.muted
        p.previous <= 0 -> "New" to c.expense
        else -> {
            val pct = ((p.current - p.previous) * 100 / p.previous).toInt()
            when {
                pct > 0 -> "+$pct%" to c.expense   // spending up = bad
                pct < 0 -> "−${-pct}%" to c.income // spending down = good
                else -> "0%" to c.muted
            }
        }
    }
    Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun TopSpendingCard(state: HomeState) {
    val c = mc
    var period by rememberSaveable { mutableIntStateOf(1) }
    val top = (if (period == 0) state.week else state.month).top
    Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 4.dp).padding(16.dp)) {
        SegmentedTabs(Periods, period, { period = it })
        if (top.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
                Text("Your top spending categories will show up here", color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
            }
        } else {
            val max = top.first().amount.coerceAtLeast(1)
            top.forEach { TopRow(it, it.amount.toFloat() / max) }
        }
    }
}

@Composable
private fun TopRow(t: TopSpend, frac: Float) {
    val c = mc
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        CategoryIcon(t.icon, t.tone, round = true)
        Column(Modifier.weight(1f)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(t.name, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                Text(Money.format(t.amount), color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            AppProgress(frac, thumb = false, fillColor = t.tone.fg)
        }
    }
}
