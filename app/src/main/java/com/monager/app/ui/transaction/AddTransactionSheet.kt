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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.AmountInput
import com.monager.app.core.Money
import com.monager.app.core.TimeRanges
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.ui.AddTransactionViewModel
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.DatePickerSheet
import com.monager.app.ui.components.TimePickerSheet
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.Tone
import com.monager.app.ui.theme.mc
import java.time.LocalDate
import java.time.ZoneId

private val Types = listOf("Chi tiêu", "Thu nhập", "Tiết kiệm")
private val KeyRows = listOf(
    listOf("C", "÷", "×", "⌫"),
    listOf("7", "8", "9", "−"),
    listOf("4", "5", "6", "+"),
    listOf("1", "2", "3", "="),
    listOf("0", "000", "k"),
)
private val Operators = setOf("÷", "×", "−", "+", "=")
private val DateFormat = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun dayLabel(d: LocalDate, today: LocalDate): String = when (d) {
    today -> "Hôm nay"
    today.minusDays(1) -> "Hôm qua"
    today.plusDays(1) -> "Ngày mai"
    else -> d.format(DateFormat)
}

@Composable
fun AddTransactionSheet(vm: AddTransactionViewModel, initialType: Int, onClose: () -> Unit) {
    val c = mc
    var type by rememberSaveable { mutableIntStateOf(initialType) }
    var expression by rememberSaveable { mutableStateOf("") }
    var parentId by rememberSaveable { mutableLongStateOf(-1L) }
    var childId by rememberSaveable { mutableLongStateOf(-1L) }
    var vaultId by rememberSaveable { mutableLongStateOf(-1L) }
    val today = remember { LocalDate.now() }
    var epochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var minuteOfDay by rememberSaveable { mutableIntStateOf(java.time.LocalTime.now().let { it.hour * 60 + it.minute }) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var note by rememberSaveable { mutableStateOf("") }

    val txType = when (type) { 1 -> TxType.INCOME; 2 -> TxType.SAVING; else -> TxType.EXPENSE }
    val tree by remember(txType) { vm.tree(if (txType == TxType.INCOME) TxType.INCOME else TxType.EXPENSE) }
        .collectAsStateWithLifecycle(emptyList())
    val wallets by vm.wallets.collectAsStateWithLifecycle()
    val vaults by vm.vaults.collectAsStateWithLifecycle()
    val input = AmountInput(expression)
    val amount = input.result ?: 0L
    val selectedNode = tree.firstOrNull { it.parent.id == parentId }
    val wallet = wallets.firstOrNull() // single implicit wallet: the UI no longer exposes wallets
    val categoryId = if (childId >= 0 && selectedNode?.children?.any { it.id == childId } == true) childId else parentId.takeIf { it >= 0 && selectedNode != null }
    val canSave = amount > 0 && wallet != null && when (txType) {
        TxType.SAVING -> vaultId >= 0 && vaults.any { it.id == vaultId }
        else -> categoryId != null
    }

    Box(
        Modifier.fillMaxSize().background(Color(0xA309190F))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .background(c.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .navigationBarsPadding().padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(c.muted.copy(alpha = .5f)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                AppIconButton(MIcon.Close, "Đóng", onClose, c.text)
                Text("Thêm giao dịch", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Lưu", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(if (canSave) 1f else .4f).clickable(enabled = canSave) { save(vm, txType, amount, categoryId, vaultId, wallet!!.id, note, LocalDate.ofEpochDay(epochDay), minuteOfDay) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClose() } }.padding(8.dp),
                )
            }
            SegmentedTabs(Types, type, { type = it; parentId = -1; childId = -1 })

            // Amount: result of the calculator; the typed expression shows above when it has operators.
            Column(Modifier.fillMaxWidth().height(78.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(if (input.hasOperator) expression else " ", color = c.muted, fontSize = 13.sp, maxLines = 1)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("₫ ", color = c.muted, fontSize = 17.sp)
                    Text(Money.plain(amount), color = c.text, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (txType == TxType.SAVING) {
                Text("Chọn quỹ tiết kiệm", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vaults, key = { it.id }, contentType = { "vault" }) { v ->
                        PickItem(MIcon.fromKey(v.iconKey), Tone.Yellow, v.name, v.id == vaultId) { vaultId = v.id }
                    }
                }
            } else {
                Text("Chọn danh mục", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tree, key = { it.parent.id }, contentType = { "category" }) { n ->
                        PickItem(MIcon.fromKey(n.parent.iconKey), Tone.fromKey(n.parent.toneKey), n.parent.name, n.parent.id == parentId) { parentId = n.parent.id; childId = -1 }
                    }
                }
                if (selectedNode != null && selectedNode.children.isNotEmpty()) {
                    LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedNode.children, key = { it.id }, contentType = { "child" }) { ch ->
                            val on = ch.id == childId
                            Text(
                                ch.name, color = if (on) c.onPrimary else c.muted, fontSize = 10.sp,
                                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) c.primary else c.surface2)
                                    .clickable { childId = if (on) -1 else ch.id }.padding(horizontal = 11.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(MIcon.Calendar, "Ngày", dayLabel(LocalDate.ofEpochDay(epochDay), today), Modifier.weight(1f)) { pickingDate = true }
                FormField(MIcon.Clock, "Giờ", "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60), Modifier.weight(1f)) { pickingTime = true }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).height(40.dp).clip(RoundedCornerShape(11.dp)).background(c.surface2).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                MIconView(MIcon.Edit, c.muted, size = 18.dp)
                Box(Modifier.weight(1f)) {
                    if (note.isEmpty()) Text("Thêm ghi chú", color = c.muted, fontSize = 11.sp)
                    BasicTextField(note, { if (it.length <= 80) note = it }, singleLine = true, textStyle = TextStyle(color = c.text, fontSize = 12.sp), cursorBrush = SolidColor(c.primary))
                }
            }

            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                KeyRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            val wide = k == "0"
                            RaisedBox(
                                Modifier.weight(if (wide) 2f else 1f).height(40.dp), radius = 10.dp,
                                onClick = { expression = AmountInput(expression).press(k).expression },
                            ) {
                                when (k) {
                                    "⌫" -> MIconView(MIcon.Delete, c.text)
                                    else -> Text(k, color = if (k in Operators) c.primary else c.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
            if (pickingDate) DatePickerSheet(epochDay, { epochDay = it }, { pickingDate = false })
            if (pickingTime) TimePickerSheet(minuteOfDay, { minuteOfDay = it }, { pickingTime = false })
            RaisedBox(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp).alpha(if (canSave) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                onClick = { if (canSave) save(vm, txType, amount, categoryId, vaultId, wallet!!.id, note, LocalDate.ofEpochDay(epochDay), minuteOfDay) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClose() } },
            ) { Text("Lưu giao dịch", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

private fun save(vm: AddTransactionViewModel, type: TxType, amount: Long, categoryId: Long?, vaultId: Long, walletId: Long, note: String, day: LocalDate, minuteOfDay: Int, onDone: () -> Unit) {
    // Date + time chosen in the form; a later date makes it a "planned" (future) transaction.
    val ts = day.atTime(minuteOfDay / 60, minuteOfDay % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    vm.save(
        TransactionEntity(
            walletId = walletId, type = type, amount = amount,
            categoryId = if (type == TxType.SAVING) null else categoryId,
            vaultId = if (type == TxType.SAVING) vaultId else null,
            note = note.trim(), timestamp = ts,
        ),
        onDone,
    )
}

@Composable
private fun PickItem(icon: MIcon, tone: Tone, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = mc
    Column(
        Modifier.width56().clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(Modifier.clip(RoundedCornerShape(12.dp)).then(if (selected) Modifier.border(2.dp, c.primary, RoundedCornerShape(12.dp)) else Modifier)) {
            CategoryIcon(icon, tone, selected = selected)
        }
        Text(label, color = if (selected) c.primary else c.muted, fontSize = 9.sp, maxLines = 1, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

private fun Modifier.width56() = this.then(Modifier.size(width = 56.dp, height = 66.dp))

@Composable
private fun FormField(icon: MIcon, label: String, value: String, modifier: Modifier, onClick: () -> Unit) {
    val c = mc
    RaisedBox(modifier.height(46.dp), radius = 11.dp, onClick = onClick, contentAlignment = Alignment.CenterStart) {
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
