package com.monager.app.ui.categoryreport

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.monager.app.core.Money
import com.monager.app.data.local.entity.TxType
import com.monager.app.di.AppContainer
import com.monager.app.ui.TxGroup
import com.monager.app.ui.components.AppProgress
import com.monager.app.ui.components.EmojiIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.Tone
import com.monager.app.ui.components.TransactionRow
import com.monager.app.ui.components.raised
import com.monager.app.ui.components.tactile
import com.monager.app.ui.groupByDay
import com.monager.app.ui.theme.mc
import com.monager.app.ui.toItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Which category group and which dates to show; [period] is the label shown under the title ("This month", "Month 11/2026"). */
@Immutable
data class CategoryReportRequest(val groupId: Long, val from: Long, val to: Long, val period: String)

/** One exact category inside the group (the parent itself or a child) with its total in the window. */
@Immutable
data class SubShare(val id: Long, val name: String, val icon: MIcon, val tone: Tone, val amount: Long)

@Immutable
data class CategoryReportState(
    val loaded: Boolean = false,
    val name: String = "",
    val icon: MIcon = MIcon.More,
    val tone: Tone = Tone.Gray,
    val income: Boolean = false,
    /** Whole group total in the window (all sub-categories). */
    val groupTotal: Long = 0,
    /** Total and count for the current sub-category filter. */
    val total: Long = 0,
    val count: Int = 0,
    val largest: Long = 0,
    val subs: List<SubShare> = emptyList(),
    val groups: List<TxGroup> = emptyList(),
)

/** All aggregation is SQL (group totals) or a single pass over one windowed row list, mapped on Dispatchers.Default. */
class CategoryReportViewModel(private val c: AppContainer) : ViewModel() {
    private val request = MutableStateFlow<CategoryReportRequest?>(null)
    private val child = MutableStateFlow(-1L)
    private val sort = MutableStateFlow(0)
    val childFilter: StateFlow<Long> = child
    val sortMode: StateFlow<Int> = sort

    fun open(r: CategoryReportRequest) {
        if (request.value == r) return
        request.value = r
        child.value = -1L
        sort.value = 0
    }
    fun selectChild(id: Long) { child.value = if (child.value == id) -1L else id }
    fun setSort(i: Int) { sort.value = i }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<CategoryReportState> = request.filterNotNull().flatMapLatest { r ->
        flow { emit(c.categories.get(r.groupId)) }.flatMapLatest { cat ->
            if (cat == null) return@flatMapLatest flowOf(CategoryReportState(loaded = true, name = "Category"))
            val rows = combine(child, sort) { ch, s -> ch to s }.flatMapLatest { (ch, s) ->
                c.transactions.rowsFiltered(r.from, r.to, r.groupId, ch, s == 1).map { Triple(ch, s, it) }
            }
            combine(c.categories.tree(cat.type), c.transactions.totalsInGroup(r.groupId, r.from, r.to), rows) { tree, totals, (ch, s, list) ->
                val today = c.today.value
                val node = tree.firstOrNull { it.parent.id == r.groupId }
                val all = node?.let { listOf(it.parent) + it.children }.orEmpty().associateBy { it.id }
                val subs = totals.mapNotNull { t ->
                    all[t.groupId]?.let { SubShare(it.id, if (it.id == r.groupId) "General" else it.name, MIcon.fromKey(it.iconKey), Tone.fromKey(it.toneKey), t.total) }
                }
                val groupTotal = totals.sumOf { it.total }
                CategoryReportState(
                    loaded = true,
                    name = cat.name, icon = MIcon.fromKey(cat.iconKey), tone = Tone.fromKey(cat.toneKey), income = cat.type == TxType.INCOME,
                    groupTotal = groupTotal,
                    total = if (ch < 0) groupTotal else totals.firstOrNull { it.groupId == ch }?.total ?: 0L,
                    count = list.size,
                    largest = list.maxOfOrNull { it.amount } ?: 0L,
                    // Chips only make sense when the money is actually split over more than the parent itself.
                    subs = if (subs.size > 1 || subs.any { it.id != r.groupId }) subs else emptyList(),
                    groups = if (s == 1) listOf(TxGroup("Largest first", list.map { it.toItem(today) })) else groupByDay(list, today),
                )
            }
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoryReportState())
}

private val SortTabs = listOf("Newest", "Largest")
private val ChipShape = RoundedCornerShape(20.dp)

/** Category drill-down: hero total, sub-category chips (with share bars), sort, and the matching transactions. */
@Composable
fun CategoryReportScreen(vm: CategoryReportViewModel, request: CategoryReportRequest, onBack: () -> Unit, onEditTx: (Long) -> Unit) {
    val c = mc
    LaunchedEffect(request) { vm.open(request) }
    val st by vm.state.collectAsStateWithLifecycle()
    val child by vm.childFilter.collectAsStateWithLifecycle()
    val sort by vm.sortMode.collectAsStateWithLifecycle()
    val amountColor = if (st.income) c.income else c.expense

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h", contentType = "header") { ScreenHeader(st.name.ifEmpty { "Category" }, request.period, onBack = onBack) }
            item(key = "hero", contentType = "hero") { Hero(st, amountColor) }
            if (st.subs.isNotEmpty()) {
                item(key = "subs", contentType = "subs") { SubBreakdown(st, child, vm::selectChild) }
            }
            item(key = "sort", contentType = "tabs") { SegmentedTabs(SortTabs, sort, vm::setSort) }
            if (st.loaded && st.groups.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 48.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MIconView(MIcon.Receipt, c.muted.copy(alpha = .6f), size = 48.dp)
                        Text("No transactions in this period", color = c.muted, fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp))
                    }
                }
            }
            st.groups.forEach { g ->
                item(key = "h_${sort}_${child}_${g.label}", contentType = "dayHeader") {
                    Text(g.label, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 4.dp))
                }
                item(key = "g_${sort}_${child}_${g.label}", contentType = "group") {
                    Column(Modifier.fillMaxWidth().raised(radius = 16.dp, depth = 3.dp).clip(RoundedCornerShape(16.dp))) {
                        g.items.forEach { TransactionRow(it, showDate = sort == 1, onClick = { onEditTx(it.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Hero(st: CategoryReportState, amountColor: Color) {
    Column(Modifier.fillMaxWidth().raised(radius = 20.dp, depth = 5.dp, brush = mc.heroBrush).padding(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = .16f)), contentAlignment = Alignment.Center) {
                EmojiIcon(st.icon, Color.White, size = 28.dp)
            }
            Column(Modifier.weight(1f)) {
                Text(if (st.income) "Total received" else "Total spent", color = Color.White.copy(alpha = .8f), fontSize = 11.sp)
                Text(Money.format(st.total), color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HeroStat("Transactions", st.count.toString(), Modifier.weight(1f))
            HeroStat("Average", Money.format(if (st.count > 0) st.total / st.count else 0L), Modifier.weight(1.3f))
            HeroStat("Largest", Money.format(st.largest), Modifier.weight(1.3f))
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = .1f)).padding(horizontal = 10.dp, vertical = 8.dp)) {
        Text(label, color = Color.White.copy(alpha = .75f), fontSize = 9.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Sub-category split: chips to filter plus a share bar per sub-category. */
@Composable
private fun SubBreakdown(st: CategoryReportState, child: Long, onSelect: (Long) -> Unit) {
    val c = mc
    Column(Modifier.fillMaxWidth().raised(radius = 16.dp, depth = 3.dp).padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("By sub-category", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            item(key = "all", contentType = "chip") { SubChip(null, "All", child < 0) { onSelect(-1L) } }
            items(st.subs, key = { it.id }, contentType = { "chip" }) { s -> SubChip(s.icon, s.name, s.id == child) { onSelect(s.id) } }
        }
        val max = st.subs.maxOfOrNull { it.amount }?.coerceAtLeast(1L) ?: 1L
        st.subs.forEach { s ->
            val on = child < 0 || child == s.id
            Column(Modifier.fillMaxWidth().tactile(RoundedCornerShape(10.dp), scale = .98f) { onSelect(s.id) }.padding(vertical = 2.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    EmojiIcon(s.icon, s.tone.fg, size = 16.dp)
                    Text(s.name, color = if (on) c.text else c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(start = 8.dp))
                    Text(
                        "${if (st.groupTotal > 0) (s.amount * 100 / st.groupTotal) else 0}%  ·  ${Money.format(s.amount)}",
                        color = if (on) c.text else c.muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
                    )
                }
                AppProgress(s.amount.toFloat() / max, thumb = false, fillColor = if (on) s.tone.fg else c.muted.copy(alpha = .4f))
            }
        }
    }
}

@Composable
private fun SubChip(icon: MIcon?, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = mc
    Row(
        Modifier.tactile(ChipShape, scale = .94f, onClick = onClick).background(if (selected) c.primary else c.surface2).padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (icon != null) EmojiIcon(icon, if (selected) c.onPrimary else c.muted, size = 14.dp)
        Text(label, color = if (selected) c.onPrimary else c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
