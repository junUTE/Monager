package com.monager.app.ui.debts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SectionTitle
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.raised
import com.monager.app.ui.sample.DebtItem
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

private val Tabs = listOf("Cho vay", "Đi vay")

@Composable
fun DebtsScreen() {
    val c = mc
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val list = if (tab == 0) SampleData.lend else SampleData.borrow
    val total = list.sumOf { it.remain }
    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h") { ScreenHeader("Khoản nợ", "Quản lý nợ & cho vay", right = { AppIconButton(MIcon.Plus, "Thêm khoản nợ", tint = c.primary) }) }
            item(key = "tabs") { SegmentedTabs(Tabs, tab, { tab = it }) }
            item(key = "sum") {
                Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 5.dp, brush = c.heroBrush).padding(18.dp)) {
                    Text(if (tab == 0) "Tổng đang cho vay" else "Tổng đang đi vay", color = Color.White.copy(alpha = .88f), fontSize = 10.sp)
                    Text(Money.format(total), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MIconView(MIcon.Users, Color.White.copy(alpha = .86f), size = 15.dp)
                        Text("${list.size} khoản đang theo dõi", color = Color.White.copy(alpha = .86f), fontSize = 9.sp)
                    }
                }
            }
            item(key = "t") { Box(Modifier.padding(top = 4.dp)) { SectionTitle("Danh sách khoản nợ", "Sắp xếp") } }
            items(list, key = { it.id }, contentType = { "debt" }) { DebtCard(it, lend = tab == 0) }
            item(key = "note") {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Box(Modifier.size(18.dp).clip(CircleShape).background(c.surface2), contentAlignment = Alignment.Center) {
                        Text("i", color = c.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("Bạn sẽ được nhắc trước ngày đến hạn 3 ngày.", color = c.muted, fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable
private fun DebtCard(d: DebtItem, lend: Boolean) {
    val c = mc
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(d.tone.fg.copy(alpha = .15f)), contentAlignment = Alignment.Center) {
                Text(d.initials, color = d.tone.fg, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(d.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Tổng khoản: ${Money.format(d.total)}", color = c.muted, fontSize = 9.sp)
            }
            MIconView(MIcon.More, c.muted)
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Còn lại", color = c.muted, fontSize = 9.sp)
                Text(Money.format(d.remain), color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f)) {
                Text("Hạn trả", color = c.muted, fontSize = 9.sp)
                Text(d.due, color = c.expense, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RaisedBox(Modifier.weight(1f).height(36.dp), radius = 10.dp, brush = c.heroBrush) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MIconView(MIcon.Check, Color.White, size = 16.dp)
                    Text(if (lend) "Thu bớt" else "Trả bớt", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            RaisedBox(Modifier.size(width = 76.dp, height = 36.dp), radius = 10.dp) {
                Text("Chi tiết", color = c.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
