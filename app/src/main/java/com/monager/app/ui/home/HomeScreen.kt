package com.monager.app.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.HomeState
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.raised
import com.monager.app.ui.sample.TxItem
import com.monager.app.ui.theme.mc

private val OnHero = Color.White
private val GlassFill = Color(0x26FFFFFF) // solid translucent instead of backdrop blur
private val GlassBorder = Color(0x47FFFFFF)

@Composable
fun HomeScreen(state: HomeState, dark: Boolean, onToggleDark: () -> Unit, onOpenCategories: () -> Unit, onAdd: (Int) -> Unit) {
    val c = mc
    Box(Modifier.fillMaxSize().background(c.homeBackground)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "top", contentType = "top") {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text("Chào buổi tối,", color = OnHero.copy(alpha = .9f), fontSize = 12.sp)
                        Text("Minh Anh", color = OnHero, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        AppIconButton(if (dark) MIcon.Sun else MIcon.Moon, "Đổi giao diện", onToggleDark, OnHero, raisedStyle = true)
                        AppIconButton(MIcon.Search, "Tìm kiếm", tint = OnHero, raisedStyle = true)
                        AppIconButton(MIcon.Settings, "Danh mục", onOpenCategories, OnHero, raisedStyle = true)
                    }
                }
            }
            item(key = "month", contentType = "month") {
                Row(
                    Modifier.clip(RoundedCornerShape(9.dp)).background(GlassFill).padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    MIconView(MIcon.Calendar, OnHero, size = 16.dp)
                    Text("Tháng ${java.time.LocalDate.now().monthValue}, ${java.time.LocalDate.now().year}", color = OnHero, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    MIconView(MIcon.ChevronDown, OnHero, size = 14.dp)
                }
            }
            item(key = "hero", contentType = "hero") { BalanceCard(state) }
            item(key = "quick", contentType = "quick") {
                Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(MIcon.ArrowDown, "Thu nhập", Modifier.weight(1f)) { onAdd(1) }
                    QuickAction(MIcon.ArrowUp, "Chi tiêu", Modifier.weight(1f)) { onAdd(0) }
                    QuickAction(MIcon.Transfer, "Chuyển khoản", Modifier.weight(1f)) { }
                }
            }
            item(key = "budget", contentType = "budget") { BudgetStrip(state) }
            item(key = "recent_title", contentType = "title") {
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Giao dịch gần đây", color = Color(0xFF17382E).takeIf { !c.dark } ?: c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text("Xem tất cả", color = if (c.dark) Color(0xFFB8DFC5) else Color(0xFF285C49), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (state.loaded && state.recent.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Text("Chưa có giao dịch. Nhấn + để thêm giao dịch đầu tiên.", color = if (c.dark) c.muted else Color(0xFF17382E), fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
                }
            }
            items(state.recent, key = { it.id }, contentType = { "tx" }) { TxRow(it) }
        }
    }
}

@Composable
private fun BalanceCard(state: HomeState) {
    Column(Modifier.fillMaxWidth().raised(radius = 16.dp, depth = 6.dp, brush = mc.heroBrush).padding(24.dp)) {
        Text("Tổng số dư", color = OnHero.copy(alpha = .88f), fontSize = 12.sp)
        Text(Money.format(state.totalBalance), color = OnHero, fontSize = 28.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth().padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Metric(MIcon.ArrowDown, "Thu nhập", Money.format(state.monthIncome), Color(0xFF9EE5B4), Modifier.weight(1f))
            Metric(MIcon.ArrowUp, "Chi tiêu", Money.format(state.monthExpense), Color(0xFFFFAD9E), Modifier.weight(1f))
        }
    }
}

@Composable
private fun Metric(icon: MIcon, label: String, value: String, tint: Color, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(13.dp)).background(Color(0x14FFFFFF)).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.size(28.dp).clip(CircleShape).background(Color(0x42041B13)), contentAlignment = Alignment.Center) {
            MIconView(icon, tint, size = 16.dp)
        }
        Column {
            Text(label, color = OnHero.copy(alpha = .88f), fontSize = 10.sp)
            Text(value, color = OnHero, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun QuickAction(icon: MIcon, label: String, modifier: Modifier, onClick: () -> Unit) {
    val c = mc
    RaisedBox(modifier.height(64.dp), radius = 16.dp, onClick = onClick) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(30.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                MIconView(icon, c.primary, size = 18.dp)
            }
            Text(label, color = c.text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

@Composable
private fun BudgetStrip(state: HomeState) {
    val limit = state.budgetLimit
    val frac = if (limit == null || limit <= 0) 0f else (state.budgetSpent.toFloat() / limit).coerceIn(0f, 1f)
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(GlassFill)
            .background(Color.Transparent).padding(16.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Color(0x47113C2C)), contentAlignment = Alignment.Center) {
                    MIconView(MIcon.Wallet, Color(0xFFF2FFF7), size = 17.dp)
                }
                Text("Hạn mức tháng", color = OnHero, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Text(if (limit == null) "—" else "${(frac * 100).toInt()}%", color = OnHero, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        AppProgress(frac, Modifier.padding(vertical = 8.dp), trackColor = Color(0x57113729), fillColor = Color(0xFFB9E3C5))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (limit == null) "Chưa đặt hạn mức tháng" else "Đã dùng ${Money.format(state.budgetSpent)}", color = OnHero.copy(alpha = .92f), fontSize = 10.sp)
            Text(if (limit == null) "" else Money.format(limit), color = OnHero.copy(alpha = .92f), fontSize = 10.sp)
        }
    }
}

@Composable
private fun TxRow(t: TxItem) {
    val c = mc
    val income = t.amount > 0
    Row(
        Modifier.fillMaxWidth().raised(radius = 16.dp, depth = 3.dp).padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(t.icon, t.tone, round = true)
        Column(Modifier.weight(1f)) {
            Text(t.title, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(t.note, color = c.muted, fontSize = 10.sp)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                (if (income) "+" else "−") + Money.format(kotlin.math.abs(t.amount)),
                color = if (income) c.income else c.expense, fontSize = 11.sp, fontWeight = FontWeight.Bold,
            )
            Text(t.date, color = c.muted, fontSize = 10.sp)
        }
    }
}
