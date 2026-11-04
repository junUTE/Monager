package com.monager.app.ui.analytics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

private val Periods = listOf("Tuần", "Tháng", "Năm")

@Composable
fun StatsScreen() {
    val c = mc
    var period by rememberSaveable { mutableIntStateOf(1) }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "h") { ScreenHeader("Báo cáo", "Theo dõi dòng tiền của bạn", right = { AppIconButton(MIcon.Calendar, "Chọn ngày", tint = c.primary) }) }
            item(key = "seg") { SegmentedTabs(Periods, period, { period = it }) }
            item(key = "donut") {
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Tổng chi tiêu", color = c.muted, fontSize = 10.sp)
                            Text("8.320.000 ₫", color = c.text, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            "↓ 12,4%", color = Color(0xFF166534), fontSize = 10.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color(0xFFE7F5EA)).padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                    Box(Modifier.fillMaxWidth().padding(vertical = 14.dp), contentAlignment = Alignment.Center) {
                        Donut(centerTop = "8,32tr", centerBottom = when (period) { 0 -> "Tuần này"; 2 -> "Năm 2026"; else -> "Tháng 10" })
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SampleData.slices.forEach { s ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.size(7.dp).clip(CircleShape).background(Color(s.argb)))
                                Text(s.name, color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f).padding(start = 8.dp))
                                Text("${s.percent}%", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(end = 12.dp))
                                Text(Money.format(s.amount), color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
            item(key = "flow") {
                AppCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Dòng tiền theo tuần", color = c.muted, fontSize = 10.sp)
                            Text("Thu nhập & chi tiêu", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.size(6.dp).background(c.income, RoundedCornerShape(2.dp)))
                            Text("Thu", color = c.muted, fontSize = 9.sp)
                            Box(Modifier.size(6.dp).background(c.expense, RoundedCornerShape(2.dp)))
                            Text("Chi", color = c.muted, fontSize = 9.sp)
                        }
                    }
                    CashFlowBars(Modifier.padding(top = 12.dp))
                }
            }
        }
    }
}

/** Lightweight Canvas donut: sweeps are computed once; nothing is allocated per frame except draw calls. */
@Composable
private fun Donut(centerTop: String, centerBottom: String) {
    val c = mc
    val slices = SampleData.slices
    val sweeps = remember { slices.map { it.percent * 3.6f } }
    val colors = remember { slices.map { Color(it.argb) } }
    Box(Modifier.size(130.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 21.dp.toPx()
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)
            var start = -90f
            for (i in sweeps.indices) {
                drawArc(colors[i], start, sweeps[i], false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                start += sweeps[i]
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(centerTop, color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(centerBottom, color = c.muted, fontSize = 9.sp)
        }
    }
}

@Composable
private fun CashFlowBars(modifier: Modifier) {
    val c = mc
    val bars = SampleData.weekBars
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(96.dp)) {
            val groupW = size.width / bars.size
            val barW = 12.dp.toPx()
            val gap = 4.dp.toPx()
            val r = CornerRadius(4.dp.toPx())
            for (g in 1..3) {
                val y = size.height * g / 4f
                drawLine(c.line, Offset(0f, y), Offset(size.width, y), 1f)
            }
            bars.forEachIndexed { i, (inc, exp) ->
                val x0 = groupW * i + (groupW - (barW * 2 + gap)) / 2f
                val hi = inc.dp.toPx()
                val he = exp.dp.toPx()
                drawRoundRect(c.income, Offset(x0, size.height - hi), Size(barW, hi), r)
                drawRoundRect(c.expense, Offset(x0 + barW + gap, size.height - he), Size(barW, he), r)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            bars.indices.forEach { Text("Tuần ${it + 1}", color = c.muted, fontSize = 9.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
        }
    }
}
