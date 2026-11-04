package com.monager.app.ui.budgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SectionTitle
import com.monager.app.ui.components.raised
import com.monager.app.ui.sample.BudgetItem
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

@Composable
fun BudgetsScreen() {
    val c = mc
    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h") { ScreenHeader("Hạn mức & Tiết kiệm", "Tháng 10, 2026", right = { AppIconButton(MIcon.Plus, "Thêm hạn mức", tint = c.primary) }) }
            item(key = "overview") { Overview() }
            item(key = "t1") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Hạn mức đang dùng", "Quản lý") } }
            items(SampleData.budgets, key = { it.id }, contentType = { "budget" }) { BudgetRow(it) }
            item(key = "t2") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Mục tiêu tiết kiệm", "+ Thêm") } }
            item(key = "vault") { VaultCard() }
        }
    }
}

@Composable
private fun Overview() {
    val c = mc
    Row(
        Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 5.dp, brush = c.heroBrush).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val w = 16.dp.toPx()
                val sz = Size(size.width - w, size.height - w)
                drawArc(Color(0x2EFFFFFF), 0f, 360f, false, Offset(w / 2, w / 2), sz, style = Stroke(w))
                drawArc(Color(0xFFB8E0C2), -90f, 360f * .65f, false, Offset(w / 2, w / 2), sz, style = Stroke(w, cap = StrokeCap.Round))
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("65%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("đã dùng", color = Color.White.copy(alpha = .88f), fontSize = 8.sp)
            }
        }
        Column {
            Text("Tổng hạn mức", color = Color.White.copy(alpha = .88f), fontSize = 10.sp)
            Text("8.320.000 ₫", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 3.dp))
            Text("trên 12.800.000 ₫", color = Color.White.copy(alpha = .88f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun BudgetRow(b: BudgetItem) {
    val c = mc
    val danger = b.pct > 90
    AppCard(padding = 12.dp) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategoryIcon(b.icon, b.tone, size = 36.dp)
            Column(Modifier.weight(1f)) {
                Text(b.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${Money.format(b.used)} / ${Money.format(b.max)}", color = c.muted, fontSize = 9.sp)
            }
            Text("${b.pct}%", color = if (danger) c.expense else c.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        AppProgress(b.pct / 100f, Modifier.padding(top = 8.dp), danger = danger)
    }
}

@Composable
private fun VaultCard() {
    val c = mc
    AppCard {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF0EADC)),
                contentAlignment = Alignment.Center,
            ) { MIconView(MIcon.Sparkles, Color(0xFF9B7734)) }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Quỹ dự phòng", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("68%", color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("34.000.000 ₫ / 50.000.000 ₫", color = c.muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                AppProgress(.68f, Modifier.padding(vertical = 4.dp))
                Text("Còn 16.000.000 ₫ để hoàn thành", color = c.muted, fontSize = 9.sp)
            }
        }
    }
}
