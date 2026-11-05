package com.monager.app.ui.debts

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.DebtMath
import com.monager.app.core.Money
import com.monager.app.core.TimeRanges
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.DateField
import com.monager.app.ui.components.InsetField
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SectionTitle
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.SimpleSheet
import com.monager.app.ui.components.raised
import com.monager.app.ui.theme.mc
import java.time.LocalDate

private val Tabs = listOf("Cho vay", "Đi vay")
private const val NONE = -1L
private const val NEW = 0L

private enum class Sheet { None, Edit, Pay, Detail }

@Composable
fun DebtsScreen(vm: DebtsViewModel, onBack: (() -> Unit)? = null) {
    val c = mc
    val st by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var sheet by rememberSaveable { mutableStateOf(Sheet.None) }
    var selected by rememberSaveable { mutableLongStateOf(NONE) }
    val lend = tab == 0
    val current = st.rows.firstOrNull { it.id == selected }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h") {
                ScreenHeader("Khoản nợ", "Quản lý nợ & cho vay", onBack = onBack, right = {
                    AppIconButton(MIcon.Plus, "Thêm khoản nợ", { selected = NEW; sheet = Sheet.Edit }, c.primary)
                })
            }
            item(key = "tabs") { SegmentedTabs(Tabs, tab, { tab = it; vm.setLend(it == 0) }) }
            item(key = "sum") {
                Column(Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 5.dp, brush = c.heroBrush).padding(18.dp)) {
                    Text(if (lend) "Còn phải thu về" else "Còn phải trả", color = Color.White.copy(alpha = .88f), fontSize = 10.sp)
                    Text(Money.format(st.totalRemaining), color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 5.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MIconView(MIcon.Users, Color.White.copy(alpha = .86f), size = 15.dp)
                        Text("${st.openCount} khoản đang theo dõi", color = Color.White.copy(alpha = .86f), fontSize = 9.sp)
                    }
                }
            }
            item(key = "t") { Box(Modifier.padding(top = 4.dp)) { SectionTitle("Danh sách khoản nợ") } }
            if (st.rows.isEmpty()) {
                item(key = "empty") { Text(if (lend) "Chưa có khoản cho vay nào. Nhấn + để thêm." else "Chưa có khoản đi vay nào. Nhấn + để thêm.", color = c.muted, fontSize = 12.sp) }
            }
            items(st.rows, key = { it.id }, contentType = { "debt" }) { d ->
                DebtCard(d, lend, onPay = { selected = d.id; sheet = Sheet.Pay }, onDetail = { selected = d.id; sheet = Sheet.Detail })
            }
        }

        when (sheet) {
            Sheet.Edit -> DebtEditSheet(
                existing = current, lend = lend,
                onSave = { name, amount, due, note, onReject ->
                    if (current == null) vm.add(name, amount, due, note) { sheet = Sheet.None }
                    else vm.edit(current.id, name, amount, due, note) { ok -> if (ok) sheet = Sheet.None else onReject() }
                },
                onClose = { sheet = Sheet.None },
            )
            Sheet.Pay -> current?.let { d ->
                PaySheet(d, lend, onPay = { amount, record, onReject -> vm.pay(d.id, amount, record) { ok -> if (ok) sheet = Sheet.None else onReject() } }, onClose = { sheet = Sheet.None })
            }
            Sheet.Detail -> current?.let { d ->
                DetailSheet(
                    d, vm,
                    onEdit = { sheet = Sheet.Edit },
                    onDelete = { vm.delete(d.id) { sheet = Sheet.None; selected = NONE } },
                    onClose = { sheet = Sheet.None },
                )
            }
            Sheet.None -> Unit
        }
    }
}

@Composable
private fun DebtCard(d: DebtRow, lend: Boolean, onPay: () -> Unit, onDetail: () -> Unit) {
    val c = mc
    AppCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(Modifier.size(38.dp).clip(CircleShape).background(c.primarySoft), contentAlignment = Alignment.Center) {
                Text(d.initials, color = c.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f)) {
                Text(d.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Tổng khoản: ${Money.format(d.total)}", color = c.muted, fontSize = 9.sp)
            }
            if (d.settled) Text("Đã xong", color = c.income, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        AppProgress(d.paid.toFloat() / d.total.coerceAtLeast(1), Modifier.padding(top = 10.dp), thumb = false)
        Row(
            Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Còn lại", color = c.muted, fontSize = 9.sp)
                Text(Money.format(d.remaining.coerceAtLeast(0)), color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Column(Modifier.weight(1f)) {
                Text("Hạn ${if (lend) "thu" else "trả"}", color = c.muted, fontSize = 9.sp)
                Text(
                    when {
                        d.dueMillis == null -> "Không có hạn"
                        d.overdueDays > 0 -> "${DebtMath.formatDate(d.dueMillis)} · quá ${d.overdueDays} ngày"
                        else -> DebtMath.formatDate(d.dueMillis)
                    },
                    color = if (d.overdueDays > 0) c.expense else c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RaisedBox(Modifier.weight(1f).height(36.dp).alpha(if (d.settled) .45f else 1f), radius = 10.dp, brush = c.heroBrush, onClick = { if (!d.settled) onPay() }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MIconView(MIcon.Check, Color.White, size = 16.dp)
                    Text(if (lend) "Thu bớt" else "Trả bớt", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
            RaisedBox(Modifier.size(width = 76.dp, height = 36.dp), radius = 10.dp, onClick = onDetail) {
                Text("Chi tiết", color = c.primary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun DebtEditSheet(
    existing: DebtRow?, lend: Boolean,
    onSave: (name: String, amount: Long, due: Long?, note: String, onReject: () -> Unit) -> Unit, onClose: () -> Unit,
) {
    val c = mc
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var digits by rememberSaveable(existing?.id) { mutableStateOf(existing?.total?.toString().orEmpty()) }
    var dueText by rememberSaveable(existing?.id) { mutableStateOf(existing?.dueMillis?.let(DebtMath::formatDate).orEmpty()) }
    var note by rememberSaveable(existing?.id) { mutableStateOf(existing?.note.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = digits.toLongOrNull() ?: 0L
    val due = DebtMath.parseDate(dueText)
    val dueOk = dueText.isBlank() || due != null
    val ok = name.isNotBlank() && amount > 0 && dueOk

    SimpleSheet(
        title = if (existing == null) (if (lend) "Thêm khoản cho vay" else "Thêm khoản đi vay") else "Sửa khoản nợ",
        onClose = onClose,
    ) {
        Label("Tên người liên quan")
        InsetField(name, { if (it.length <= 40) name = it }, "Ví dụ: Hoàng Nam")
        Label("Số tiền")
        InsetField(digits, { v -> if (v.length <= 12 && v.all { it.isDigit() }) digits = v }, "Ví dụ 5000000", numeric = true)
        if (amount > 0) Text(Money.format(amount), color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        Label("Ngày hẹn (có thể bỏ trống)")
        DateField(dueText, { dueText = it }, "Không có hạn", clearable = true)
        Label("Ghi chú")
        InsetField(note, { if (it.length <= 80) note = it }, "Không bắt buộc")
        error?.let { Text(it, color = c.expense, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
        RaisedBox(
            Modifier.fillMaxWidth().padding(top = 14.dp).height(44.dp).alpha(if (ok) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
            onClick = {
                if (ok) onSave(name, amount, due?.let { TimeRanges.startOfDay(it) }, note) { error = "Tổng khoản không được nhỏ hơn số đã ${if (lend) "thu" else "trả"} (${Money.format(existing?.paid ?: 0)})." }
            },
        ) { Text("Lưu", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun PaySheet(d: DebtRow, lend: Boolean, onPay: (amount: Long, record: Boolean, onReject: () -> Unit) -> Unit, onClose: () -> Unit) {
    val c = mc
    var digits by rememberSaveable(d.id) { mutableStateOf("") }
    var record by rememberSaveable(d.id) { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val amount = digits.toLongOrNull() ?: 0L
    val ok = amount in 1..d.remaining

    SimpleSheet(if (lend) "Thu bớt từ ${d.name}" else "Trả bớt cho ${d.name}", "Còn lại ${Money.format(d.remaining)}", onClose) {
        Label("Số tiền")
        InsetField(digits, { v -> if (v.length <= 12 && v.all { it.isDigit() }) { digits = v; error = null } }, "Nhập số tiền", numeric = true)
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuickChip("Một nửa") { digits = (d.remaining / 2).coerceAtLeast(1).toString() }
            QuickChip(if (lend) "Thu hết" else "Trả hết") { digits = d.remaining.toString() }
        }
        if (amount > d.remaining) Text("Vượt quá số còn lại", color = c.expense, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp).clickable { record = !record },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.size(22.dp).clip(RoundedCornerShape(6.dp)).background(if (record) c.primary else c.surface2),
                contentAlignment = Alignment.Center,
            ) { if (record) MIconView(MIcon.Check, c.onPrimary, size = 14.dp) }
            Text(if (lend) "Ghi vào giao dịch thu nhập (ví đầu tiên)" else "Ghi vào giao dịch chi tiêu (ví đầu tiên)", color = c.text, fontSize = 11.sp)
        }
        error?.let { Text(it, color = c.expense, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }
        RaisedBox(
            Modifier.fillMaxWidth().padding(top = 14.dp).height(44.dp).alpha(if (ok) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
            onClick = { if (ok) onPay(amount, record) { error = "Không ghi được khoản này." } },
        ) { Text("Xác nhận", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DetailSheet(d: DebtRow, vm: DebtsViewModel, onEdit: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit) {
    val c = mc
    val payments by remember(d.id) { vm.payments(d.id) }.collectAsStateWithLifecycle(emptyList())
    var confirm by remember { mutableStateOf(false) }
    SimpleSheet(d.name, "Tổng ${Money.format(d.total)} · đã ${Money.format(d.paid)} · còn ${Money.format(d.remaining.coerceAtLeast(0))}", onClose) {
        if (d.note.isNotBlank()) Text(d.note, color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
        Text("Lịch sử", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
        if (payments.isEmpty()) Text("Chưa có lần thanh toán nào.", color = c.muted, fontSize = 12.sp)
        payments.take(8).forEach { p ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(DebtMath.formatDate(p.timestamp), color = c.muted, fontSize = 12.sp)
                Text(Money.format(p.amount), color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (payments.size > 8) Text("… và ${payments.size - 8} lần khác", color = c.muted, fontSize = 10.sp)
        if (confirm) Text("Xóa khoản nợ và lịch sử thanh toán? Các giao dịch đã ghi trước đó vẫn được giữ.", color = c.expense, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RaisedBox(Modifier.size(width = 52.dp, height = 44.dp), radius = 13.dp, onClick = { if (confirm) onDelete() else confirm = true }) {
                MIconView(MIcon.Delete, c.expense)
            }
            RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, brush = c.heroBrush, onClick = onEdit) {
                Text("Sửa khoản nợ", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(text, color = mc.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    val c = mc
    Text(label, color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(c.primarySoft).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp))
}
