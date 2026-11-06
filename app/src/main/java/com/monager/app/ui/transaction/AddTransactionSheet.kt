package com.monager.app.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.runtime.LaunchedEffect
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
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.EmojiIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.Tone
import com.monager.app.ui.theme.mc
import java.time.LocalDate
import java.time.ZoneId

private val Types = listOf("Expense", "Income", "Savings")
private val KeyRows = listOf(
    listOf("C", "÷", "×", "⌫"),
    listOf("7", "8", "9", "−"),
    listOf("4", "5", "6", "+"),
)
// Bottom block: digits on the left, one tall "=" key on the right (spans the last two rows).
private val BottomLeft = listOf(listOf("1", "2", "3"), listOf("0", "000"))
private val Operators = setOf("÷", "×", "−", "+", "=")
private val DateFormat = java.time.format.DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", java.util.Locale.ENGLISH)

/** Small note next to the concrete date; null for any other day. */
private fun dayNote(d: LocalDate, today: LocalDate): String? = when (d) {
    today -> "Today"
    today.minusDays(1) -> "Yesterday"
    today.plusDays(1) -> "Tomorrow"
    else -> null
}

@Composable
fun AddTransactionSheet(vm: AddTransactionViewModel, initialType: Int, onClose: () -> Unit, editId: Long = -1L) {
    val c = mc
    var type by rememberSaveable { mutableIntStateOf(initialType) }
    var expression by rememberSaveable { mutableStateOf("") }
    var parentId by rememberSaveable { mutableLongStateOf(-1L) }
    var childId by rememberSaveable { mutableLongStateOf(-1L) }
    var vaultId by rememberSaveable { mutableLongStateOf(-1L) }
    val today = remember { LocalDate.now() }
    var epochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    var note by rememberSaveable { mutableStateOf("") }

    // Edit mode: load the row once, prefill the form (only the first time, so edits survive rotation).
    val editing = editId >= 0
    var original by remember { mutableStateOf<TransactionEntity?>(null) }
    var prefilled by rememberSaveable(editId) { mutableStateOf(!editing) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(editId) {
        if (!editing) return@LaunchedEffect
        val loaded = vm.load(editId)
        if (loaded == null) { onClose(); return@LaunchedEffect }
        val (tx, cat) = loaded
        original = tx
        if (!prefilled) {
            type = when (tx.type) { TxType.INCOME -> 1; TxType.SAVING -> 2; else -> 0 }
            expression = tx.amount.toString()
            if (cat?.parentId != null) { parentId = cat.parentId; childId = cat.id } else if (cat != null) { parentId = cat.id; childId = -1L }
            vaultId = tx.vaultId ?: -1L
            epochDay = TimeRanges.toDate(tx.timestamp).toEpochDay()
            note = tx.note
            prefilled = true
        }
    }
    val isTransfer = original?.type == TxType.TRANSFER // transfers have no editor here; they can only be deleted

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
    val canSave = prefilled && !isTransfer && !input.hasOperator && amount > 0 && wallet != null && when (txType) {
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
                // imePadding lifts the sheet above the keyboard; the scroll keeps the note field reachable on short screens.
                .imePadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 12.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(c.muted.copy(alpha = .5f)))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                AppIconButton(MIcon.Close, "Close", onClose, c.text)
                Text(if (editing) "Edit transaction" else "Add transaction", color = c.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Save", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.alpha(if (canSave) 1f else .4f).clickable(enabled = canSave) { save(vm, original, txType, amount, categoryId, vaultId, wallet!!.id, note, LocalDate.ofEpochDay(epochDay)) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClose() } }.padding(8.dp),
                )
            }
            SegmentedTabs(Types, type, { type = it; parentId = -1; childId = -1 })

            // Amount display, right-aligned. While an expression is being typed it is shown as typed; the result only
            // appears after pressing "=" (which collapses the expression into a plain number).
            val pending = input.hasOperator
            val shown = remember(expression) { expression.replace("+", " + ").replace("−", " − ").replace("×", " × ").replace("÷", " ÷ ") }
            Row(Modifier.fillMaxWidth().height(70.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
                if (pending) {
                    Text(
                        shown, color = c.text, fontSize = if (shown.length <= 14) 34.sp else if (shown.length <= 22) 28.sp else 22.sp,
                        fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.End,
                    )
                } else {
                    Text(Money.mark + " ", color = c.muted, fontSize = 20.sp)
                    Text(Money.plain(amount), color = c.text, fontSize = 34.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                }
            }

            if (txType == TxType.SAVING) {
                Text("Choose savings goal", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(vaults, key = { it.id }, contentType = { "vault" }) { v ->
                        PickItem(MIcon.fromKey(v.iconKey), Tone.Yellow, v.name, v.id == vaultId) { vaultId = v.id }
                    }
                }
            } else {
                Text("Choose category", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(bottom = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(tree, key = { it.parent.id }, contentType = { "category" }) { n ->
                        PickItem(MIcon.fromKey(n.parent.iconKey), Tone.fromKey(n.parent.toneKey), n.parent.name, n.parent.id == parentId) { parentId = n.parent.id; childId = -1 }
                    }
                }
                if (selectedNode != null && selectedNode.children.isNotEmpty()) {
                    LazyRow(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(selectedNode.children, key = { it.id }, contentType = { "child" }) { ch ->
                            val on = ch.id == childId
                            Row(
                                Modifier.clip(RoundedCornerShape(20.dp)).background(if (on) c.primary else c.surface2)
                                    .clickable { childId = if (on) -1 else ch.id }.padding(start = 9.dp, end = 11.dp, top = 5.dp, bottom = 5.dp),
                                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                EmojiIcon(MIcon.fromKey(ch.iconKey), if (on) c.onPrimary else Tone.fromKey(ch.toneKey).fg, size = 14.dp)
                                Text(ch.name, color = if (on) c.onPrimary else c.muted, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DayStep(MIcon.Back) { epochDay-- }
                DateField(LocalDate.ofEpochDay(epochDay), today, Modifier.weight(1f)) { pickingDate = true }
                DayStep(MIcon.ChevronRight) { epochDay++ }
            }
            // The whole box is the tap target (not just the text): tapping anywhere focuses the field.
            val noteFocus = remember { FocusRequester() }
            var noteFocused by remember { mutableStateOf(false) }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2)
                    .border(1.5.dp, if (noteFocused) c.primary else Color.Transparent, RoundedCornerShape(12.dp))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { noteFocus.requestFocus() }
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MIconView(MIcon.Edit, if (noteFocused) c.primary else c.muted, size = 18.dp)
                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    // The hint disappears as soon as the field is focused.
                    if (note.isEmpty() && !noteFocused) Text("Add a note", color = c.muted, fontSize = 12.sp)
                    BasicTextField(
                        note, { if (it.length <= 80) note = it }, singleLine = true,
                        textStyle = TextStyle(color = c.text, fontSize = 13.sp), cursorBrush = SolidColor(c.primary),
                        modifier = Modifier.fillMaxWidth().focusRequester(noteFocus).onFocusChanged { noteFocused = it.isFocused },
                    )
                }
            }

            Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                val press = { k: String -> expression = AmountInput(expression).press(k).expression }
                KeyRows.forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { k ->
                            RaisedBox(Modifier.weight(1f).height(40.dp), radius = 10.dp, onClick = { press(k) }) {
                                when (k) {
                                    "⌫" -> MIconView(MIcon.Backspace, c.text, size = 22.dp)
                                    else -> Text(k, color = if (k in Operators) c.primary else c.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        BottomLeft.forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                row.forEach { k ->
                                    RaisedBox(Modifier.weight(if (k == "0") 2f else 1f).height(40.dp), radius = 10.dp, onClick = { press(k) }) {
                                        Text(k, color = c.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
                                    }
                                }
                            }
                        }
                    }
                    RaisedBox(Modifier.weight(1f).height(87.dp), radius = 10.dp, brush = c.heroBrush, onClick = { press("=") }) {
                        Text("=", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
            if (pickingDate) DatePickerSheet(epochDay, { epochDay = it }, { pickingDate = false })
            RaisedBox(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp).alpha(if (canSave) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                onClick = { if (canSave) save(vm, original, txType, amount, categoryId, vaultId, wallet!!.id, note, LocalDate.ofEpochDay(epochDay)) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClose() } },
            ) { Text(if (editing) "Save changes" else "Save transaction", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            if (editing && original != null) {
                if (isTransfer) Text("Transfers can't be edited, only deleted.", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                RaisedBox(Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp), radius = 13.dp, onClick = { confirmDelete = true }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MIconView(MIcon.Delete, c.expense, size = 18.dp)
                        Text("Delete transaction", color = c.expense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (confirmDelete) {
                androidx.compose.material3.AlertDialog(
                    onDismissRequest = { confirmDelete = false },
                    title = { Text("Delete this transaction?") },
                    text = { Text("This can't be undone.") },
                    confirmButton = {
                        androidx.compose.material3.TextButton(onClick = { original?.let { o -> vm.delete(o) { confirmDelete = false; onClose() } } }) { Text("Delete", color = c.expense) }
                    },
                    dismissButton = { androidx.compose.material3.TextButton(onClick = { confirmDelete = false }) { Text("Keep") } },
                )
            }
        }
    }
}

private fun save(vm: AddTransactionViewModel, original: TransactionEntity?, type: TxType, amount: Long, categoryId: Long?, vaultId: Long, walletId: Long, note: String, day: LocalDate, onDone: () -> Unit) {
    val zone = ZoneId.systemDefault()
    // New rows: time of day = now (same-day rows keep entry order). Edits keep the original time of day; a later date makes it "planned".
    val time = original?.let { java.time.Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalTime() } ?: java.time.LocalTime.now().withNano(0)
    val ts = day.atTime(time).atZone(zone).toInstant().toEpochMilli()
    val tx = (original ?: TransactionEntity(walletId = walletId, type = type, amount = amount, timestamp = ts)).copy(
        type = type, amount = amount,
        categoryId = if (type == TxType.SAVING) null else categoryId,
        vaultId = if (type == TxType.SAVING) vaultId else null,
        note = note.trim(), timestamp = ts,
    )
    if (original != null) vm.update(tx, onDone) else vm.save(tx, onDone)
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
private fun DayStep(icon: MIcon, onClick: () -> Unit) {
    RaisedBox(Modifier.size(46.dp), radius = 11.dp, onClick = onClick) { MIconView(icon, mc.primary, size = 20.dp) }
}

/** Concrete date (e.g. "Fri, 06/11/2026") with a small pill for Today / Yesterday / Tomorrow. Tap opens the calendar. */
@Composable
private fun DateField(date: LocalDate, today: LocalDate, modifier: Modifier, onClick: () -> Unit) {
    val c = mc
    val note = dayNote(date, today)
    RaisedBox(modifier.height(46.dp), radius = 11.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MIconView(MIcon.Calendar, c.primary, size = 18.dp)
            Text(date.format(DateFormat), color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
            if (note != null) {
                Text(
                    note, color = c.primary, fontSize = 9.sp, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.primarySoft).padding(horizontal = 7.dp, vertical = 3.dp),
                )
            }
        }
    }
}

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
