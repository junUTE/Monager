package com.monager.app.ui.transactions

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.MillisRange
import com.monager.app.core.Money
import com.monager.app.ui.FilterChipUi
import com.monager.app.ui.TransactionsViewModel
import com.monager.app.ui.components.EmojiIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.TransactionRow
import com.monager.app.ui.components.raised
import com.monager.app.ui.components.tactile
import com.monager.app.ui.theme.mc

private val Tabs = listOf("Last month", "This month", "Upcoming")
private val ChipShape = RoundedCornerShape(20.dp)
private val CardShape = RoundedCornerShape(16.dp)

@Composable
fun TransactionsScreen(
    vm: TransactionsViewModel,
    hideBalance: Boolean,
    onEdit: (Long) -> Unit,
    onOpenReports: () -> Unit = {},
    onOpenCategory: (id: Long, range: MillisRange, period: String) -> Unit = { _, _, _ -> },
) {
    val c = mc
    val state by vm.state.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()
    val category by vm.categoryFilter.collectAsStateWithLifecycle()
    val filters by vm.filters.collectAsStateWithLifecycle()
    val active = filters.firstOrNull { it.id == category }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
        ) {
            item(key = "top", contentType = "top") {
                Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Transactions", color = c.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    // Shortcut to the full Reports screen.
                    RaisedBox(Modifier.height(36.dp), radius = 12.dp, onClick = onOpenReports) {
                        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MIconView(MIcon.Chart, c.primary, size = 16.dp)
                            Text("Reports", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            item(key = "balance", contentType = "balance") {
                Column(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Balance", color = c.muted, fontSize = 12.sp)
                    Text(
                        if (hideBalance) "••••••••" else Money.format(state.balance),
                        color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
            item(key = "tabs", contentType = "tabs") {
                SegmentedTabs(Tabs, selected, vm::select, Modifier.padding(bottom = 10.dp))
            }
            if (filters.isNotEmpty()) {
                item(key = "filters", contentType = "filters") {
                    LazyRow(Modifier.padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item(key = "all", contentType = "chip") { FilterChip(null, "All", category < 0) { vm.selectCategory(-1L) } }
                        items(filters, key = { it.id }, contentType = { "chip" }) { f -> FilterChip(f, f.name, f.id == category) { vm.selectCategory(f.id) } }
                    }
                }
            }
            if (active != null && state.loaded) {
                item(key = "summary", contentType = "summary") {
                    FilterSummary(active, state.count, state.filteredTotal) { onOpenCategory(active.id, state.range, state.rangeLabel) }
                }
            }
            if (state.loaded && state.groups.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MIconView(MIcon.Receipt, c.muted.copy(alpha = .6f), size = 56.dp)
                        Text(if (active != null) "No ${active.name} transactions here" else "No transactions yet", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            state.groups.forEach { g ->
                item(key = "h_${selected}_${category}_${g.label}", contentType = "header") {
                    Text(g.label, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                }
                item(key = "g_${selected}_${category}_${g.label}", contentType = "group") {
                    // One raised card per day group: a single cheap shadow instead of one per row; clipped so ripples stay rounded.
                    Column(Modifier.fillMaxWidth().raised(radius = 16.dp, depth = 3.dp).clip(CardShape)) {
                        g.items.forEach { TransactionRow(it, showDate = false, onClick = { onEdit(it.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterChip(f: FilterChipUi?, label: String, selected: Boolean, onClick: () -> Unit) {
    val c = mc
    Row(
        Modifier.tactile(ChipShape, scale = .94f, onClick = onClick).background(if (selected) c.primary else c.surface2)
            .padding(start = if (f != null) 9.dp else 13.dp, end = 13.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (f != null) EmojiIcon(f.icon, if (selected) c.onPrimary else f.tone.fg, size = 15.dp)
        Text(label, color = if (selected) c.onPrimary else c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** Filter result line with a button into the category report for the same window. */
@Composable
private fun FilterSummary(f: FilterChipUi, count: Int, total: Long, onOpenReport: () -> Unit) {
    val c = mc
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp).raised(radius = 14.dp, depth = 3.dp).padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        EmojiIcon(f.icon, f.tone.fg, size = 20.dp)
        Column(Modifier.weight(1f)) {
            Text(f.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("$count transactions · ${Money.format(total)}", color = c.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        RaisedBox(Modifier.height(34.dp), radius = 11.dp, brush = c.heroBrush, onClick = onOpenReport) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Report", color = androidx.compose.ui.graphics.Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                MIconView(MIcon.ChevronRight, androidx.compose.ui.graphics.Color.White, size = 14.dp)
            }
        }
    }
}
