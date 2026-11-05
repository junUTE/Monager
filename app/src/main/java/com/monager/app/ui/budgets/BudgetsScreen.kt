package com.monager.app.ui.budgets

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.Money
import com.monager.app.ui.BudgetRow
import com.monager.app.ui.BudgetsViewModel
import com.monager.app.ui.VaultRow
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.InsetField
import com.monager.app.ui.components.LimitSlider
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SectionTitle
import com.monager.app.ui.components.SimpleSheet
import com.monager.app.ui.components.raised
import com.monager.app.ui.theme.mc
import java.time.LocalDate

private const val NONE = -1L
private const val OVERALL = -2L
private const val NEW_VAULT = 0L

@Composable
fun BudgetsScreen(vm: BudgetsViewModel) {
    val c = mc
    val st by vm.state.collectAsStateWithLifecycle()
    // Which sheet is open: budget (editBudget = budget id, OVERALL = new overall, NEW = pick category) or vault.
    var budgetSheet by rememberSaveable { mutableStateOf(false) }
    var editBudgetId by rememberSaveable { mutableLongStateOf(NONE) }
    var vaultSheet by rememberSaveable { mutableStateOf(false) }
    var editVaultId by rememberSaveable { mutableLongStateOf(NEW_VAULT) }
    val today = LocalDate.now()

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h") {
                ScreenHeader("Hạn mức & Tiết kiệm", "Tháng ${today.monthValue}, ${today.year}", right = {
                    AppIconButton(MIcon.Plus, "Thêm hạn mức", { editBudgetId = NONE; budgetSheet = true }, c.primary)
                })
            }
            item(key = "overview") {
                Overview(st.overall) { editBudgetId = st.overall?.id ?: NONE; budgetSheet = true }
            }
            if (st.overall == null && st.rows.isEmpty() && st.previousCount > 0) {
                item(key = "rollover", contentType = "rollover") { RolloverCard(st.previousCount) { vm.copyLastMonth {} } }
            }
            item(key = "t1") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Hạn mức theo danh mục") } }
            if (st.rows.isEmpty()) {
                item(key = "empty1") { Text("Chưa có hạn mức theo danh mục. Nhấn + để thêm.", color = c.muted, fontSize = 12.sp) }
            }
            items(st.rows, key = { it.id }, contentType = { "budget" }) { row ->
                BudgetCard(row) { editBudgetId = row.id; budgetSheet = true }
            }
            item(key = "t2") {
                Box(Modifier.padding(top = 6.dp).clickable { editVaultId = NEW_VAULT; vaultSheet = true }) { SectionTitle("Mục tiêu tiết kiệm", "+ Thêm") }
            }
            if (st.vaults.isEmpty()) {
                item(key = "empty2") { Text("Chưa có quỹ tiết kiệm.", color = c.muted, fontSize = 12.sp) }
            }
            items(st.vaults, key = { "v${it.id}" }, contentType = { "vault" }) { v ->
                VaultCard(v) { editVaultId = v.id; vaultSheet = true }
            }
        }

        if (budgetSheet) {
            val existing = (listOfNotNull(st.overall) + st.rows).firstOrNull { it.id == editBudgetId }
            BudgetEditSheet(
                existing = existing, st = st,
                onSave = { catId, limit -> vm.setBudget(catId, limit) { budgetSheet = false } },
                onDelete = existing?.let { e -> { vm.removeBudget(e.id) { budgetSheet = false } } },
                onClose = { budgetSheet = false },
            )
        }
        if (vaultSheet) {
            val existing = st.vaults.firstOrNull { it.id == editVaultId }
            VaultEditSheet(
                existing, { name, target -> vm.saveVault(existing?.id ?: NEW_VAULT, name, target) { vaultSheet = false } },
                existing?.let { e -> { vm.deleteVault(e.id) { vaultSheet = false } } }, { vaultSheet = false },
            )
        }
    }
}

@Composable
private fun RolloverCard(count: Int, onClick: () -> Unit) {
    val c = mc
    AppCard {
        Text("Tháng mới chưa có hạn mức", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Dùng lại $count hạn mức của tháng trước (chỉ sao chép số tiền tối đa).", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        RaisedBox(Modifier.fillMaxWidth().padding(top = 10.dp).height(40.dp), radius = 12.dp, brush = c.heroBrush, onClick = onClick) {
            Text("Dùng lại hạn mức tháng trước", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Overview(overall: BudgetRow?, onClick: () -> Unit) {
    val frac = if (overall == null || overall.limit <= 0) 0f else (overall.spent.toFloat() / overall.limit).coerceIn(0f, 1f)
    Row(
        Modifier.fillMaxWidth().raised(radius = 18.dp, depth = 5.dp, brush = mc.heroBrush).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Box(Modifier.size(84.dp), contentAlignment = Alignment.Center) {
            // Stroke objects are created once (not per draw); geometry below is value-class math only.
            val w = with(androidx.compose.ui.platform.LocalDensity.current) { 16.dp.toPx() }
            val trackStroke = androidx.compose.runtime.remember(w) { Stroke(w) }
            val arcStroke = androidx.compose.runtime.remember(w) { Stroke(w, cap = StrokeCap.Round) }
            Canvas(Modifier.fillMaxSize()) {
                val sz = Size(size.width - w, size.height - w)
                drawArc(Color(0x2EFFFFFF), 0f, 360f, false, Offset(w / 2, w / 2), sz, style = trackStroke)
                drawArc(if (frac >= .9f) Color(0xFFFFA39A) else Color(0xFFB8E0C2), -90f, 360f * frac, false, Offset(w / 2, w / 2), sz, style = arcStroke)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(if (overall == null) "—" else "${overall.pct}%", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("đã dùng", color = Color.White.copy(alpha = .88f), fontSize = 8.sp)
            }
        }
        Column {
            Text("Tổng hạn mức", color = Color.White.copy(alpha = .88f), fontSize = 10.sp)
            if (overall == null) {
                Text("Chạm để đặt hạn mức tháng", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
            } else {
                Text(Money.format(overall.spent), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 3.dp))
                Text("trên ${Money.format(overall.limit)}", color = Color.White.copy(alpha = .88f), fontSize = 9.sp)
            }
        }
    }
}

@Composable
private fun BudgetCard(b: BudgetRow, onClick: () -> Unit) {
    val c = mc
    val danger = b.pct >= 90
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AppCard(padding = 12.dp) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CategoryIcon(b.icon, b.tone, size = 36.dp)
                Column(Modifier.weight(1f)) {
                    Text(b.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${Money.format(b.spent)} / ${Money.format(b.limit)}", color = c.muted, fontSize = 9.sp)
                }
                Text("${b.pct}%", color = if (danger) c.expense else c.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            AppProgress(b.spent.toFloat() / b.limit.coerceAtLeast(1), Modifier.padding(top = 8.dp), danger = danger)
        }
    }
}

@Composable
private fun VaultCard(v: VaultRow, onClick: () -> Unit) {
    val c = mc
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        AppCard {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF0EADC)), contentAlignment = Alignment.Center) {
                    MIconView(MIcon.fromKey(v.iconKey), Color(0xFF9B7734))
                }
                Column(Modifier.weight(1f)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(v.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("${v.pct}%", color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("${Money.format(v.saved)} / ${Money.format(v.target)}", color = c.muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                    AppProgress(v.saved.toFloat() / v.target.coerceAtLeast(1), Modifier.padding(vertical = 4.dp))
                    Text(
                        if (v.saved >= v.target) "Đã hoàn thành mục tiêu" else "Còn ${Money.format(v.target - v.saved)} để hoàn thành",
                        color = c.muted, fontSize = 9.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetEditSheet(
    existing: BudgetRow?, st: com.monager.app.ui.BudgetsState,
    onSave: (categoryId: Long?, limit: Long) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit,
) {
    val c = mc
    var digits by rememberSaveable(existing?.id) { mutableStateOf(existing?.limit?.toString().orEmpty()) }
    var pickedCat by rememberSaveable(existing?.id) { mutableLongStateOf(OVERALL) }
    // Targets when creating: the overall budget (if not set) and every expense parent without a budget.
    val pick = if (pickedCat == OVERALL && st.overall != null) (st.available.firstOrNull()?.id ?: NONE) else pickedCat
    val creatingOverall = existing == null && pick == OVERALL
    val limit = digits.toLongOrNull() ?: 0L
    val target: Long? = if (existing != null) existing.categoryId else if (creatingOverall) null else pick
    val canSave = limit > 0 && (existing != null || creatingOverall || pick != NONE)

    SimpleSheet(
        title = existing?.let { "Sửa hạn mức: ${it.name}" } ?: "Thêm hạn mức",
        subtitle = "Áp dụng cho tháng hiện tại",
        onClose = onClose,
    ) {
        if (existing == null) {
            Text("Áp dụng cho", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (st.overall == null) {
                    item(key = "all") { Chip("Tổng tháng", pick == OVERALL) { pickedCat = OVERALL } }
                }
                items(st.available, key = { it.id }, contentType = { "chip" }) { cat -> Chip(cat.name, pick == cat.id) { pickedCat = cat.id } }
            }
        }
        Text("Số tiền tối đa / tháng", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        InsetField(digits, { v -> if (v.length <= 12 && v.all { it.isDigit() }) digits = v }, "Ví dụ 5000000", numeric = true)
        if (limit > 0) Text(Money.format(limit), color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        // Drag the thumb to set the limit (typing above stays available for exact amounts). The range is fixed when the sheet opens.
        val sliderMax = androidx.compose.runtime.remember(existing?.id) { maxOf(10_000_000L, ((limit * 2 + 999_999) / 1_000_000) * 1_000_000) }
        LimitSlider(limit, sliderMax, if (sliderMax <= 20_000_000L) 100_000L else 500_000L, { digits = it.toString() }, Modifier.padding(top = 8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("0", color = c.muted, fontSize = 9.sp)
            Text(Money.format(sliderMax), color = c.muted, fontSize = 9.sp)
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (onDelete != null) {
                RaisedBox(Modifier.size(width = 52.dp, height = 44.dp), radius = 13.dp, onClick = onDelete) { MIconView(MIcon.Delete, c.expense) }
            }
            RaisedBox(
                Modifier.weight(1f).height(44.dp).alpha(if (canSave) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                onClick = { if (canSave) onSave(target, limit) },
            ) { Text("Lưu", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = mc
    Text(
        label, color = if (selected) c.onPrimary else c.muted, fontSize = 11.sp,
        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (selected) c.primary else c.surface2).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun VaultEditSheet(existing: VaultRow?, onSave: (String, Long) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val c = mc
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var digits by rememberSaveable(existing?.id) { mutableStateOf(existing?.target?.toString().orEmpty()) }
    val target = digits.toLongOrNull() ?: 0L
    val ok = name.isNotBlank() && target > 0
    var confirmDelete by rememberSaveable(existing?.id) { mutableStateOf(false) }
    SimpleSheet(if (existing == null) "Thêm quỹ tiết kiệm" else "Sửa quỹ tiết kiệm", "Gửi tiền vào quỹ bằng giao dịch loại Tiết kiệm", onClose) {
        Text("Tên quỹ", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        InsetField(name, { if (it.length <= 30) name = it }, "Ví dụ: Mua laptop")
        Text("Mục tiêu", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        InsetField(digits, { v -> if (v.length <= 12 && v.all { it.isDigit() }) digits = v }, "Ví dụ 20000000", numeric = true)
        if (target > 0) Text(Money.format(target), color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 6.dp))
        if (confirmDelete && existing != null && onDelete != null) {
            Text(
                "Xóa quỹ \"${existing.name}\"? ${Money.format(existing.saved)} đã gửi vào quỹ sẽ được trả lại vào số dư. Không hoàn tác được.",
                color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = { confirmDelete = false }) {
                    Text("Giữ lại", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = onDelete) {
                    Text("Xóa quỹ", color = c.expense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing != null && onDelete != null) {
                    RaisedBox(Modifier.size(width = 52.dp, height = 44.dp), radius = 13.dp, onClick = { confirmDelete = true }) { MIconView(MIcon.Delete, c.expense) }
                }
                RaisedBox(
                    Modifier.weight(1f).height(44.dp).alpha(if (ok) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                    onClick = { if (ok) onSave(name, target) },
                ) { Text("Lưu", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
