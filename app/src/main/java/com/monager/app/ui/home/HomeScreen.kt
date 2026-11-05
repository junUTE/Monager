package com.monager.app.ui.home

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import com.monager.app.ui.components.raised
import com.monager.app.ui.theme.mc

private val Periods = listOf("Tuần", "Tháng")

@Composable
fun HomeScreen(
    state: HomeState,
    hideBalance: Boolean,
    onToggleHide: () -> Unit,
    onSeeReports: () -> Unit,
    onSeeTransactions: () -> Unit,
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
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (hideBalance) "••••••••" else Money.format(state.totalBalance),
                            color = c.text, fontSize = 28.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("Tổng số dư", color = c.muted, fontSize = 12.sp)
                    }
                    AppIconButton(if (hideBalance) MIcon.EyeOff else MIcon.Eye, "Ẩn/hiện số dư", onToggleHide, c.text)
                }
            }

            item(key = "report_title", contentType = "title") { Section("Báo cáo tháng này", "Xem báo cáo", onSeeReports) }
            item(key = "report", contentType = "report") { MonthReportCard(state) }

            item(key = "compare", contentType = "compare") { CompareCard(state) }

            item(key = "top_title", contentType = "title") { Section("Chi tiêu nhiều nhất", "Xem chi tiết", onSeeReports) }
            item(key = "top", contentType = "top") { TopSpendingCard(state) }

            item(key = "recent_title", contentType = "title") { Section("Giao dịch gần đây", "Xem tất cả", onSeeTransactions) }
            item(key = "recent", contentType = "recent") {
                Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 4.dp).padding(vertical = 4.dp)) {
                    if (state.loaded && state.recent.isEmpty()) {
                        Text("Chưa có giao dịch. Nhấn + để thêm giao dịch đầu tiên.", color = c.muted, fontSize = 12.sp, modifier = Modifier.padding(16.dp))
                    }
                    state.recent.forEach { TransactionRow(it, showDate = true) }
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

@Composable
private fun MonthReportCard(state: HomeState) {
    val c = mc
    val spent = state.month.current
    val income = state.monthIncome
    val total = spent + income
    // Share of spending in (spent + income), read in the draw phase.
    val frac = if (total <= 0) 0f else spent.toFloat() / total
    Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 4.dp).padding(16.dp)) {
        Row(Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tổng chi", color = c.muted, fontSize = 12.sp)
                Text(Money.format(spent), color = c.expense, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Tổng thu", color = c.muted, fontSize = 12.sp)
                Text(Money.format(income), color = c.income, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }
        val track = c.surface2
        val expense = c.expense
        val incomeColor = c.income
        Canvas(Modifier.fillMaxWidth().padding(top = 12.dp).height(4.dp)) {
            val r = CornerRadius(size.height / 2f)
            if (total <= 0) {
                drawRoundRect(track, Offset.Zero, size, r)
            } else {
                drawRoundRect(incomeColor, Offset.Zero, size, r)
                drawRoundRect(expense, Offset.Zero, Size(size.width * frac, size.height), r)
            }
        }
        if (state.dailyExpense.isEmpty()) {
            Box(Modifier.fillMaxWidth().height(130.dp), contentAlignment = Alignment.Center) {
                Text("Nhập giao dịch để xem báo cáo", color = c.muted, fontSize = 13.sp)
            }
        } else {
            val values = state.dailyExpense
            Canvas(Modifier.fillMaxWidth().padding(top = 16.dp).height(130.dp)) {
                val slot = size.width / values.size
                val barW = slot * 0.6f
                val r = CornerRadius(barW / 2f)
                values.forEachIndexed { i, v ->
                    val h = (size.height * v).coerceAtLeast(if (v > 0f) 4.dp.toPx() else 2.dp.toPx())
                    drawRoundRect(
                        if (v > 0f) expense else track,
                        Offset(i * slot + (slot - barW) / 2f, size.height - h), Size(barW, h), r,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompareCard(state: HomeState) {
    val c = mc
    var period by rememberSaveable { mutableIntStateOf(1) }
    val p = if (period == 0) state.week else state.month
    val labels = if (period == 0) "Tuần trước" to "Tuần này" else "Tháng trước" to "Tháng này"
    Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 4.dp).padding(16.dp)) {
        SegmentedTabs(Periods, period, { period = it })
        Text(Money.format(p.current), color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp), maxLines = 1)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (period == 0) "Tổng chi tuần này" else "Tổng chi tháng này", color = c.muted, fontSize = 12.sp)
            ChangeBadge(p)
        }
        val max = maxOf(p.current, p.previous)
        val prevFrac = if (max <= 0) 0f else p.previous.toFloat() / max
        val curFrac = if (max <= 0) 0f else p.current.toFloat() / max
        val barColor = c.surface2
        val fill = c.primary
        Canvas(Modifier.fillMaxWidth().padding(top = 16.dp).height(110.dp)) {
            val barW = size.width * 0.22f
            val r = CornerRadius(8.dp.toPx())
            val x0 = size.width * 0.25f - barW / 2f
            val x1 = size.width * 0.75f - barW / 2f
            drawRoundRect(barColor, Offset(x0, 0f), Size(barW, size.height), r)
            drawRoundRect(barColor, Offset(x1, 0f), Size(barW, size.height), r)
            val ph = (size.height * prevFrac).coerceAtLeast(if (p.previous > 0) 6.dp.toPx() else 0f)
            val ch = (size.height * curFrac).coerceAtLeast(if (p.current > 0) 6.dp.toPx() else 0f)
            drawRoundRect(fill.copy(alpha = .55f), Offset(x0, size.height - ph), Size(barW, ph), r)
            drawRoundRect(fill, Offset(x1, size.height - ch), Size(barW, ch), r)
        }
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
        p.previous <= 0 -> "Mới" to c.expense
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
                Text("Các danh mục chi nhiều nhất sẽ hiện ở đây", color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
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
