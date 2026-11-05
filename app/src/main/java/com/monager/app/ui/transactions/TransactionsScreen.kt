package com.monager.app.ui.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.Money
import com.monager.app.ui.TransactionsViewModel
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.TransactionRow
import com.monager.app.ui.components.raised
import com.monager.app.ui.theme.mc

private val Tabs = listOf("Tháng trước", "Tháng này", "Tương lai")

@Composable
fun TransactionsScreen(vm: TransactionsViewModel, hideBalance: Boolean) {
    val c = mc
    val state by vm.state.collectAsStateWithLifecycle()
    val selected by vm.selected.collectAsStateWithLifecycle()

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        ) {
            item(key = "balance", contentType = "balance") {
                Column(Modifier.fillMaxWidth().padding(bottom = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Số dư", color = c.muted, fontSize = 12.sp)
                    Text(
                        if (hideBalance) "••••••••" else Money.format(state.balance),
                        color = c.text, fontSize = 24.sp, fontWeight = FontWeight.Bold,
                    )
                }
            }
            item(key = "tabs", contentType = "tabs") {
                SegmentedTabs(Tabs, selected, vm::select, Modifier.padding(bottom = 14.dp))
            }
            if (state.loaded && state.groups.isEmpty()) {
                item(key = "empty", contentType = "empty") {
                    Column(Modifier.fillMaxWidth().padding(top = 80.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        MIconView(MIcon.Receipt, c.muted.copy(alpha = .6f), size = 56.dp)
                        Text("Chưa có giao dịch", color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 12.dp))
                    }
                }
            }
            state.groups.forEach { g ->
                item(key = "h_${selected}_${g.label}", contentType = "header") {
                    Text(g.label, color = c.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
                }
                item(key = "g_${selected}_${g.label}", contentType = "group") {
                    // One raised card per day group: a single cheap shadow instead of one per row.
                    Column(Modifier.fillMaxWidth().raised(radius = 16.dp, depth = 3.dp)) {
                        g.items.forEach { TransactionRow(it, showDate = false) }
                    }
                }
            }
        }
    }
}
