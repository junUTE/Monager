package com.monager.app.ui.budgets

import com.monager.app.ui.components.tactile
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
import androidx.compose.foundation.layout.heightIn
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
import com.monager.app.ui.components.IconPicker
import com.monager.app.ui.components.InsetField
import com.monager.app.ui.components.LimitSlider
import com.monager.app.ui.components.drawBudgetRing
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.EmojiIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SectionTitle
import com.monager.app.ui.components.SimpleSheet
import com.monager.app.ui.components.raised
import com.monager.app.ui.components.pressScale
import com.monager.app.ui.components.rememberPress
import com.monager.app.ui.components.rememberReveal
import androidx.compose.foundation.LocalIndication
import androidx.compose.runtime.remember
import androidx.compose.ui.text.style.TextOverflow
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
    // Overall-budget detail sheet (lists the sub-budgets); editing a sub-budget from it returns here afterwards.
    var detailSheet by rememberSaveable { mutableStateOf(false) }
    var backToDetail by rememberSaveable { mutableStateOf(false) }
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
                ScreenHeader("Budgets & Savings", "Month ${today.monthValue}, ${today.year}")
            }
            item(key = "overview") {
                Overview(st.overall, today.lengthOfMonth() - today.dayOfMonth + 1) {
                    if (st.overall == null) { editBudgetId = NONE; backToDetail = false; budgetSheet = true } else detailSheet = true
                }
            }
            if (st.overall == null && st.rows.isEmpty() && st.previousCount > 0) {
                item(key = "rollover", contentType = "rollover") { RolloverCard(st.previousCount) { vm.copyLastMonth {} } }
            }
            item(key = "t1") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Sub-budgets") } }
            if (st.rows.isEmpty()) {
                item(key = "empty1") { Text(if (st.overall == null) "Set the monthly total budget first, then split it by category." else "Tap the total budget to split it by category.", color = c.muted, fontSize = 12.sp) }
            }
            items(st.rows, key = { it.id }, contentType = { "budget" }) { row ->
                BudgetCard(row, today.lengthOfMonth() - today.dayOfMonth + 1) { editBudgetId = row.id; backToDetail = false; budgetSheet = true }
            }
            val other = st.other
            if (other != null && st.rows.isNotEmpty()) {
                item(key = "other", contentType = "budget") { BudgetCard(other, today.lengthOfMonth() - today.dayOfMonth + 1) { detailSheet = true } }
            }
            item(key = "t2") {
                Box(Modifier.padding(top = 6.dp)) { SectionTitle("Savings goals", "+ Add", onAction = { editVaultId = NEW_VAULT; vaultSheet = true }) }
            }
            if (st.vaults.isEmpty()) {
                item(key = "empty2") { Text("No savings goals yet.", color = c.muted, fontSize = 12.sp) }
            }
            items(st.vaults, key = { "v${it.id}" }, contentType = { "vault" }) { v ->
                VaultCard(v) { editVaultId = v.id; vaultSheet = true }
            }
        }

        if (detailSheet && st.overall != null) {
            OverallDetailSheet(
                st = st,
                onEditTotal = { detailSheet = false; editBudgetId = st.overall!!.id; backToDetail = true; budgetSheet = true },
                onAdd = { detailSheet = false; editBudgetId = NONE; backToDetail = true; budgetSheet = true },
                onEditChild = { id -> detailSheet = false; editBudgetId = id; backToDetail = true; budgetSheet = true },
                onClose = { detailSheet = false },
            )
        }
        if (budgetSheet) {
            val existing = (listOfNotNull(st.overall) + st.rows).firstOrNull { it.id == editBudgetId }
            val closeBudget = { budgetSheet = false; if (backToDetail) { detailSheet = true; backToDetail = false } }
            BudgetEditSheet(
                existing = existing, st = st,
                onSave = { catId, limit -> vm.setBudget(catId, limit) { closeBudget() } },
                onDelete = existing?.let { e -> { vm.removeBudget(e.id, e.categoryId == null) { closeBudget() } } },
                onClose = { closeBudget() },
            )
        }
        if (vaultSheet) {
            val existing = st.vaults.firstOrNull { it.id == editVaultId }
            VaultEditSheet(
                existing, { name, target, icon -> vm.saveVault(existing?.id ?: NEW_VAULT, name, target, icon) { vaultSheet = false } },
                existing?.let { e -> { vm.deleteVault(e.id) { vaultSheet = false } } }, { vaultSheet = false },
            )
        }
    }
}

@Composable
private fun RolloverCard(count: Int, onClick: () -> Unit) {
    val c = mc
    AppCard {
        Text("No budget set for this month", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Text("Reuse $count budgets from last month (only the maximum amounts are copied).", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
        RaisedBox(Modifier.fillMaxWidth().padding(top = 10.dp).height(40.dp), radius = 12.dp, brush = c.heroBrush, onClick = onClick) {
            Text("Reuse last month's budgets", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/** How far a budget is used: drives colours and the status text everywhere on this screen. */
private enum class Level { Ok, Near, Over }

private fun level(b: BudgetRow): Level = when {
    b.limit > 0 && b.spent > b.limit -> Level.Over
    b.pct >= 80 -> Level.Near
    else -> Level.Ok
}

// Ring/pill colours on the dark emerald hero (fixed: the hero brush is the same in light and dark theme).
private val HeroMint = Color(0xFFB8E0C2)
private val HeroAmber = Color(0xFFFFD27A)
private val HeroCoral = Color(0xFFFFA39A)
private val HeroRed = Color(0xFFFF6B5E)

/**
 * Monthly total: ring + status. Over 100% the ring keeps going as a second, deeper-red lap (capped at 200%) so
 * "how much over" stays readable instead of a ring that is simply full.
 */
@Composable
private fun Overview(overall: BudgetRow?, daysLeft: Int, onClick: () -> Unit) {
    val frac = if (overall == null || overall.limit <= 0) 0f else overall.spent.toFloat() / overall.limit
    val lvl = overall?.let { level(it) } ?: Level.Ok
    val press = rememberPress()
    val shape = remember { RoundedCornerShape(20.dp) }
    Column(
        Modifier.fillMaxWidth().pressScale(press, .98f).raised(radius = 20.dp, depth = 5.dp, brush = mc.heroBrush, press = press)
            .clip(shape).clickable(press.source, LocalIndication.current, onClick = onClick).padding(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
            Box(Modifier.size(104.dp), contentAlignment = Alignment.Center) {
                // Stroke objects are created once (not per draw); geometry below is value-class math only.
                val w = with(androidx.compose.ui.platform.LocalDensity.current) { 14.dp.toPx() }
                val trackStroke = remember(w) { Stroke(w) }
                val arcStroke = remember(w) { Stroke(w, cap = StrokeCap.Round) }
                val ringColor = when (lvl) { Level.Ok -> HeroMint; Level.Near -> HeroAmber; Level.Over -> HeroCoral }
                val reveal = rememberReveal(overall?.id, ready = overall != null, durationMillis = 1000)
                Canvas(Modifier.fillMaxSize()) { drawBudgetRing(frac, w, trackStroke, arcStroke, Color(0x2EFFFFFF), ringColor, HeroRed, reveal.value) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(if (overall == null) "—" else "${overall.pct}%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("used", color = Color.White.copy(alpha = .85f), fontSize = 10.sp)
                }
            }
            Column(Modifier.weight(1f)) {
                Text("Total budget", color = Color.White.copy(alpha = .85f), fontSize = 11.sp)
                if (overall == null) {
                    Text("Tap to set monthly budget", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                } else {
                    Text(Money.format(overall.spent), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.padding(vertical = 2.dp))
                    Text("of ${Money.format(overall.limit)}", color = Color.White.copy(alpha = .85f), fontSize = 11.sp, maxLines = 1)
                    val (label, color) = when (lvl) {
                        Level.Ok -> "On track" to HeroMint
                        Level.Near -> (if (overall.spent >= overall.limit) "Limit reached" else "Near limit") to HeroAmber
                        Level.Over -> "Over budget" to HeroCoral
                    }
                    Text(
                        label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp).clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = .18f)).padding(horizontal = 9.dp, vertical = 4.dp),
                    )
                }
            }
        }
        if (overall != null) {
            val left = overall.limit - overall.spent
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeroStat(if (left < 0) "Over by" else "Left", Money.format(kotlin.math.abs(left)), if (left < 0) HeroCoral else Color.White, Modifier.weight(1.2f))
                HeroStat("Per day", if (left > 0 && daysLeft > 0) Money.format(left / daysLeft) else "—", Color.White, Modifier.weight(1.2f))
                HeroStat("Days left", "$daysLeft", Color.White, Modifier.weight(.8f))
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, valueColor: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .1f)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, color = Color.White.copy(alpha = .75f), fontSize = 9.sp, maxLines = 1)
        Text(value, color = valueColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * Sub-budget card. Colours step up green -> amber (80%) -> red (over). Over 100% the bar stays full: the part up to
 * the limit tick is soft red and the overflow solid red, with "Over by X" underneath.
 */
@Composable
private fun BudgetCard(b: BudgetRow, daysLeft: Int, onClick: () -> Unit) {
    val c = mc
    val lvl = level(b)
    val color = when (lvl) { Level.Ok -> c.primary; Level.Near -> c.warning; Level.Over -> c.expense }
    AppCard(padding = 14.dp, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryIcon(b.icon, b.tone, size = 40.dp)
            Column(Modifier.weight(1f)) {
                Text(b.name, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("${Money.format(b.spent)} of ${Money.format(b.limit)}", color = c.muted, fontSize = 11.sp, maxLines = 1)
            }
            Text(
                "${b.pct}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(color.copy(alpha = .13f)).padding(horizontal = 9.dp, vertical = 4.dp),
            )
        }
        AppProgress(
            b.spent.toFloat() / b.limit.coerceAtLeast(1), Modifier.padding(top = 10.dp), thumb = false,
            fillColor = if (lvl == Level.Over) c.expense.copy(alpha = .45f) else color, overColor = c.expense,
        )
        val left = b.limit - b.spent
        Row(Modifier.fillMaxWidth().padding(top = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            if (left < 0) {
                Text("Over by ${Money.format(-left)}", color = c.expense, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("${Money.format(left)} left", color = c.muted, fontSize = 11.sp)
            }
            if (left > 0 && daysLeft > 0) Text("≈ ${Money.format(left / daysLeft)}/day", color = c.muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun VaultCard(v: VaultRow, onClick: () -> Unit) {
    val c = mc
    AppCard(onClick = onClick) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(42.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF0EADC)), contentAlignment = Alignment.Center) {
                EmojiIcon(MIcon.fromKey(v.iconKey), Color(0xFF9B7734))
            }
            Column(Modifier.weight(1f)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(v.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text("${v.pct}%", color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Text("${Money.format(v.saved)} / ${Money.format(v.target)}", color = c.muted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
                AppProgress((v.saved.toFloat() / v.target.coerceAtLeast(1)).coerceAtMost(1f), Modifier.padding(vertical = 4.dp), fillColor = if (v.saved >= v.target) c.income else c.primary)
                Text(
                    if (v.saved >= v.target) "Goal reached" else "${Money.format(v.target - v.saved)} to go",
                    color = if (v.saved >= v.target) c.income else c.muted, fontSize = 9.sp,
                )
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
    // Sub-budgets need a total first: with no overall budget yet, the sheet can only create the overall one.
    val pick = if (st.overall == null) OVERALL else if (pickedCat == OVERALL) (st.available.firstOrNull()?.id ?: NONE) else pickedCat
    val creatingOverall = existing == null && pick == OVERALL
    val editingOverall = creatingOverall || (existing != null && existing.categoryId == null)
    val limit = digits.toLongOrNull() ?: 0L
    val target: Long? = if (existing != null) existing.categoryId else if (creatingOverall) null else pick
    // Sub-budgets must add up to at most the total (the rest is "Other"); the total cannot drop below the sum of its subs.
    val subsTotal = st.rows.sumOf { it.limit }
    val minLimit = if (editingOverall) subsTotal else 0L
    val maxLimit = if (editingOverall || st.overall == null) Long.MAX_VALUE else st.overall.limit - (subsTotal - (existing?.limit ?: 0L))
    val canSave = limit > 0 && limit >= minLimit && limit <= maxLimit && (existing != null || creatingOverall || pick != NONE)

    SimpleSheet(
        title = existing?.let { "Edit budget: ${it.name}" } ?: if (creatingOverall) "Set total budget" else "Add sub-budget",
        subtitle = "Applies to the current month",
        onClose = onClose,
        centered = true,
    ) {
        if (existing == null && st.overall != null) {
            Text("Applies to", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (st.overall == null) {
                    item(key = "all") { Chip("Monthly total", pick == OVERALL) { pickedCat = OVERALL } }
                }
                items(st.available, key = { it.id }, contentType = { "chip" }) { cat -> Chip(cat.name, pick == cat.id) { pickedCat = cat.id } }
            }
        }
        Text("Maximum amount / month", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        InsetField(digits, { v -> if (v.length <= 12 && v.all { it.isDigit() }) digits = v }, "e.g. 5,000,000", autoFocus = true, money = true)
        if (!editingOverall && st.overall != null) {
            Text(
                if (limit > maxLimit) "Exceeds what is left of the total budget (max ${Money.format(maxLimit.coerceAtLeast(0))})" else "Up to ${Money.format(maxLimit.coerceAtLeast(0))} left in the total budget",
                color = if (limit > maxLimit || maxLimit <= 0) c.expense else c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp),
            )
        } else if (editingOverall && subsTotal > 0) {
            Text(
                "Sub-budgets already allocate ${Money.format(subsTotal)}; the total cannot be lower",
                color = if (limit in 1 until minLimit) c.expense else c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp),
            )
        }
        // Drag the thumb to set the limit (typing above stays available for exact amounts). The range is fixed when the sheet opens.
        // The total budget has no natural range, so it is typed. Sub-budgets are bounded by what is left of the total.
        if (!editingOverall && maxLimit != Long.MAX_VALUE) {
            val sliderMax = androidx.compose.runtime.remember(existing?.id) { maxLimit.coerceAtLeast(100_000L) }
            LimitSlider(limit, sliderMax, if (sliderMax <= 20_000_000L) 100_000L else 500_000L, { digits = it.toString() }, Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("0", color = c.muted, fontSize = 9.sp)
                Text(Money.format(sliderMax), color = c.muted, fontSize = 9.sp)
            }
        }
        var confirmDelete by rememberSaveable(existing?.id) { mutableStateOf(false) }
        if (confirmDelete && existing != null && onDelete != null) {
            Text(
                if (existing.categoryId == null) "Delete this month's total budget${if (st.rows.isNotEmpty()) " and ${st.rows.size} sub-budgets" else ""}? This cannot be undone."
                else "Delete budget \"${existing.name}\"? Its share will count under Other. This cannot be undone.",
                color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = { confirmDelete = false }) {
                    Text("Keep", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = onDelete) {
                    Text("Delete", color = c.expense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    RaisedBox(Modifier.size(width = 52.dp, height = 44.dp), radius = 13.dp, onClick = { confirmDelete = true }) { MIconView(MIcon.Delete, c.expense) }
                }
                RaisedBox(
                    Modifier.weight(1f).height(44.dp).alpha(if (canSave) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                    onClick = { if (canSave) onSave(target, limit) },
                ) { Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, onClick: () -> Unit) {
    val c = mc
    Text(
        label, color = if (selected) c.onPrimary else c.muted, fontSize = 11.sp,
        modifier = Modifier.tactile(RoundedCornerShape(20.dp), scale = .94f, onClick = onClick).background(if (selected) c.primary else c.surface2).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun VaultEditSheet(existing: VaultRow?, onSave: (String, Long, String) -> Unit, onDelete: (() -> Unit)?, onClose: () -> Unit) {
    val c = mc
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var digits by rememberSaveable(existing?.id) { mutableStateOf(existing?.target?.toString().orEmpty()) }
    var icon by rememberSaveable(existing?.id) { mutableStateOf(existing?.iconKey ?: MIcon.Sparkles.name) }
    val target = digits.toLongOrNull() ?: 0L
    val ok = name.isNotBlank() && target > 0
    var confirmDelete by rememberSaveable(existing?.id) { mutableStateOf(false) }
    SimpleSheet(if (existing == null) "Add savings goal" else "Edit savings goal", "Add money to a goal with a Savings transaction", onClose, centered = true) {
        Text("Goal name", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        InsetField(name, { if (it.length <= 30) name = it }, "e.g. Buy a laptop", autoFocus = true)
        Text("Target", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        InsetField(digits, { v -> if (v.length <= 12 && v.all { it.isDigit() }) digits = v }, "e.g. 20,000,000", money = true)
        Text("Icon", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
        IconPicker(icon, { icon = it })
        if (confirmDelete && existing != null && onDelete != null) {
            Text(
                "Delete goal \"${existing.name}\"? ${Money.format(existing.saved)} saved in it will be returned to your balance. This cannot be undone.",
                color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 14.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = { confirmDelete = false }) {
                    Text("Keep", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = onDelete) {
                    Text("Delete goal", color = c.expense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (existing != null && onDelete != null) {
                    RaisedBox(Modifier.size(width = 52.dp, height = 44.dp), radius = 13.dp, onClick = { confirmDelete = true }) { MIconView(MIcon.Delete, c.expense) }
                }
                RaisedBox(
                    Modifier.weight(1f).height(44.dp).alpha(if (ok) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                    onClick = { if (ok) onSave(name, target, icon) },
                ) { Text("Save", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}

/** Overall budget detail: total, the sub-budgets, and the unlabeled remainder ("Other"). */
@Composable
private fun OverallDetailSheet(
    st: com.monager.app.ui.BudgetsState, onEditTotal: () -> Unit, onAdd: () -> Unit, onEditChild: (Long) -> Unit, onClose: () -> Unit,
) {
    val c = mc
    val overall = st.overall ?: return
    val other = st.other
    SimpleSheet("Total budget", "Split by category; the unallocated part counts as Other", onClose, centered = true) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(Money.format(overall.limit), color = c.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("Spent ${Money.format(overall.spent)} (${overall.pct}%)", color = c.muted, fontSize = 11.sp)
            }
            RaisedBox(Modifier.size(width = 84.dp, height = 36.dp), radius = 11.dp, onClick = onEditTotal) {
                Text("Edit total", color = c.primary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        androidx.compose.foundation.lazy.LazyColumn(Modifier.padding(top = 10.dp).fillMaxWidth().heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(st.rows, key = { it.id }, contentType = { "sub" }) { r -> SubRow(r.name, r.limit, r.spent, r.icon, r.tone) { onEditChild(r.id) } }
            if (other != null) item(key = "other", contentType = "sub") { SubRow(other.name, other.limit, other.spent, other.icon, other.tone, null) }
        }
        val canAdd = st.available.isNotEmpty() && (other?.limit ?: 0L) > 0
        RaisedBox(
            Modifier.fillMaxWidth().padding(top = 12.dp).height(44.dp).alpha(if (canAdd) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
            onClick = { if (canAdd) onAdd() },
        ) { Text(if (canAdd) "+ Add sub-budget" else "Total budget fully allocated", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SubRow(name: String, limit: Long, spent: Long, icon: MIcon, tone: com.monager.app.ui.components.Tone, onClick: (() -> Unit)?) {
    val c = mc
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.tactile(RoundedCornerShape(12.dp), scale = .98f, onClick = onClick) else Modifier.clip(RoundedCornerShape(12.dp))).background(c.surface2).padding(10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        CategoryIcon(icon, tone, size = 32.dp)
        Column(Modifier.weight(1f)) {
            Text(name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (limit in 1 until spent) "Spent ${Money.format(spent)} · over by ${Money.format(spent - limit)}" else "Spent ${Money.format(spent)}",
                color = if (limit in 1 until spent) c.expense else c.muted, fontSize = 9.sp,
            )
        }
        Text(Money.format(limit), color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
