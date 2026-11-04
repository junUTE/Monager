package com.monager.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

private val Types = listOf("Chi tiêu", "Thu nhập", "Tiết kiệm")
private val Keys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "000", "0", "del")
private const val MaxDigits = 12 // keeps the value inside Long range

@Composable
fun AddTransactionSheet(initialType: Int, onClose: () -> Unit) {
    val c = mc
    var type by rememberSaveable { mutableIntStateOf(initialType) }
    var digits by rememberSaveable { mutableStateOf("250000") }
    var category by rememberSaveable { mutableIntStateOf(0) }

    Box(
        Modifier.fillMaxSize().background(Color(0xA309190F))
            .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(c.bg)
                .clickable(interactionSource = MutableInteractionSource(), indication = null) {}
                .navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(c.muted.copy(alpha = .5f)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                AppIconButton(MIcon.Close, "Đóng", onClose, c.text)
                Text("Thêm giao dịch", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("Lưu", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(onClick = onClose).padding(8.dp))
            }
            SegmentedTabs(Types, type, { type = it })
            Row(Modifier.fillMaxWidth().height(72.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Text("₫ ", color = c.muted, fontSize = 17.sp)
                Text(Money.plain(digits.toLongOrNull() ?: 0L), color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
            Text("Chọn danh mục", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SampleData.pickCategories.forEachIndexed { i, p ->
                    Column(
                        Modifier.weight(1f).clickable(interactionSource = MutableInteractionSource(), indication = null) { category = i },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            Modifier.clip(RoundedCornerShape(12.dp)).then(
                                if (i == category) Modifier.border(2.dp, c.primary, RoundedCornerShape(12.dp)) else Modifier,
                            ),
                        ) { CategoryIcon(p.icon, p.tone, selected = i == category) }
                        Text(
                            p.name, color = if (i == category) c.primary else c.muted, fontSize = 9.sp, maxLines = 1,
                            fontWeight = if (i == category) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(MIcon.Calendar, "Ngày", "Hôm nay, 26/10", Modifier.weight(1f))
                FormField(MIcon.Wallet, "Ví", "Tiền mặt", Modifier.weight(1f))
            }
            RaisedBox(Modifier.fillMaxWidth().padding(top = 8.dp).height(40.dp), radius = 11.dp, contentAlignment = Alignment.CenterStart) {
                Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    MIconView(MIcon.Edit, c.muted, size = 18.dp)
                    Text("Thêm ghi chú", color = c.muted, fontSize = 11.sp)
                }
            }
            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Keys.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { k ->
                            RaisedBox(Modifier.weight(1f).height(42.dp), radius = 10.dp, onClick = {
                                digits = when {
                                    k == "del" -> digits.dropLast(1).ifEmpty { "0" }
                                    digits.length + k.length > MaxDigits -> digits
                                    digits == "0" -> k.trimStart('0').ifEmpty { "0" }
                                    else -> digits + k
                                }
                            }) {
                                if (k == "del") MIconView(MIcon.Delete, c.text)
                                else Text(k, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
            RaisedBox(Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp), radius = 13.dp, brush = c.heroBrush, onClick = onClose) {
                Text("Lưu giao dịch", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun FormField(icon: MIcon, label: String, value: String, modifier: Modifier) {
    val c = mc
    RaisedBox(modifier.height(46.dp), radius = 11.dp, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal = 9.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            MIconView(icon, c.primary, size = 18.dp)
            Column(Modifier.weight(1f, fill = false)) {
                Text(label, color = c.muted, fontSize = 8.sp)
                Text(value, color = c.text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            }
            MIconView(MIcon.ChevronRight, c.muted, size = 16.dp)
        }
    }
}
