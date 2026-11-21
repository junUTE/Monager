package com.monager.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monager.app.ui.AddTransactionViewModel
import com.monager.app.ui.AppViewModelFactory
import com.monager.app.ui.HomeViewModel
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.monager.app.di.AppContainer
import com.monager.app.ui.analytics.StatsScreen
import com.monager.app.ui.budgets.BudgetsScreen
import com.monager.app.ui.categories.CategoriesScreen
import com.monager.app.ui.components.LocalOpenSheets
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.pressScale
import com.monager.app.ui.components.rememberPress
import com.monager.app.ui.categoryreport.CategoryReportRequest
import com.monager.app.ui.categoryreport.CategoryReportScreen
import com.monager.app.ui.debts.DebtsScreen
import com.monager.app.ui.home.HomeScreen
import com.monager.app.ui.settings.SettingsScreen
import com.monager.app.ui.transactions.TransactionsScreen
import com.monager.app.ui.theme.mc
import com.monager.app.ui.transaction.AddTransactionSheet

enum class Tab(val icon: MIcon, val label: String) {
    Home(MIcon.Home, "Home"), Transactions(MIcon.Receipt, "Transactions"), Budgets(MIcon.Wallet, "Budgets"), Account(MIcon.Settings, "Settings")
}

/** State-driven navigation: 4 tabs + Stats/Debts/Categories sub-screens + Add sheet. No NavHost overhead for such a small graph. */
@Composable
fun MonagerNavHost(container: AppContainer, dark: Boolean) {
    val c = mc
    val factory = remember(container) { AppViewModelFactory(container) }
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val homeState by homeVm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(Tab.Home.ordinal) }
    var inCategories by rememberSaveable { mutableStateOf(false) }
    var inStats by rememberSaveable { mutableStateOf(false) }
    var inDebts by rememberSaveable { mutableStateOf(false) }
    var hideBalance by rememberSaveable { mutableStateOf(false) }
    var addType by rememberSaveable { mutableIntStateOf(-1) } // -1 = sheet closed
    var editTxId by rememberSaveable { mutableLongStateOf(-1L) } // >= 0: editing that transaction in the same sheet
    val openSheets = remember { mutableIntStateOf(0) }
    // Category report (drill-down from Home top spending, Reports or the Transactions filter); groupId < 0 = closed.
    var catGroup by rememberSaveable { mutableLongStateOf(-1L) }
    var catFrom by rememberSaveable { mutableLongStateOf(0L) }
    var catTo by rememberSaveable { mutableLongStateOf(0L) }
    var catPeriod by rememberSaveable { mutableStateOf("") }
    val openCategory = { id: Long, from: Long, to: Long, period: String -> catFrom = from; catTo = to; catPeriod = period; catGroup = id }
    val inCategoryReport = catGroup >= 0

    val inSub = inCategories || inStats || inDebts || inCategoryReport
    // System back: close the add sheet, then the open sub-screen.
    BackHandler(enabled = addType >= 0 || editTxId >= 0 || inSub) {
        when {
            addType >= 0 || editTxId >= 0 -> { addType = -1; editTxId = -1 }
            inCategoryReport -> catGroup = -1L
            inCategories -> inCategories = false
            inStats -> inStats = false
            else -> inDebts = false
        }
    }
    // Dark status-bar icons on the light theme, light icons on the dark theme.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !dark
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }

    CompositionLocalProvider(LocalOpenSheets provides openSheets) {
    Box(Modifier.fillMaxSize().background(c.bg)) {
        when {
            inCategoryReport -> CategoryReportScreen(
                viewModel(factory = factory), CategoryReportRequest(catGroup, catFrom, catTo, catPeriod),
                onBack = { catGroup = -1L }, onEditTx = { editTxId = it },
            )
            inCategories -> CategoriesScreen(viewModel(factory = factory), onBack = { inCategories = false })
            inStats -> StatsScreen(viewModel(factory = factory), onBack = { inStats = false }, onOpenCategory = openCategory)
            inDebts -> DebtsScreen(viewModel(factory = factory), onBack = { inDebts = false })
            tab == Tab.Home.ordinal -> HomeScreen(
                homeState, hideBalance, { hideBalance = !hideBalance },
                onSeeReports = { inStats = true }, onSeeTransactions = { tab = Tab.Transactions.ordinal }, onEditTx = { editTxId = it },
                onOpenCategory = { id, r, period -> openCategory(id, r.from, r.to, period) },
            )
            tab == Tab.Transactions.ordinal -> TransactionsScreen(
                viewModel(factory = factory), hideBalance, onEdit = { editTxId = it },
                onOpenReports = { inStats = true },
                onOpenCategory = { id, r, period -> openCategory(id, r.from, r.to, period) },
            )
            tab == Tab.Budgets.ordinal -> BudgetsScreen(viewModel(factory = factory))
            else -> SettingsScreen(
                viewModel(factory = factory),
                onOpenCategories = { inCategories = true }, onOpenDebts = { inDebts = true }, onOpenStats = { inStats = true },
            )
        }
        if (!inSub && openSheets.intValue == 0) {
            BottomNav(
                selected = tab,
                onSelect = { tab = it },
                onAdd = { addType = 0 },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (addType >= 0 || editTxId >= 0) {
            val addVm: AddTransactionViewModel = viewModel(factory = factory)
            AddTransactionSheet(addVm, initialType = addType.coerceAtLeast(0), editId = editTxId, onClose = { addType = -1; editTxId = -1 })
        }
    }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    val c = mc
    Box(modifier.fillMaxWidth().background(c.surface.copy(alpha = .94f)).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem(Tab.Home, selected, onSelect, Modifier.weight(1f))
            NavItem(Tab.Transactions, selected, onSelect, Modifier.weight(1f))
            Box(Modifier.weight(1f))
            NavItem(Tab.Budgets, selected, onSelect, Modifier.weight(1f))
            NavItem(Tab.Account, selected, onSelect, Modifier.weight(1f))
        }
        RaisedBox(
            Modifier.align(Alignment.TopCenter).offset(y = (-22).dp).size(52.dp),
            radius = 18.dp, brush = c.heroBrush, onClick = onAdd,
        ) { MIconView(MIcon.Plus, Color.White, size = 26.dp) }
    }
}

@Composable
private fun NavItem(tab: Tab, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier) {
    val c = mc
    val on = tab.ordinal == selected
    val tint = if (on) c.primary else c.muted.copy(alpha = .75f)
    // Press feedback: the icon pill dips and springs back (layer phase only); no full-height square ripple.
    val press = rememberPress()
    Column(
        modifier.height(64.dp).clickable(interactionSource = press.source, indication = null) { onSelect(tab.ordinal) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Selected tab: tinted pill behind the icon (flat fill, no shadow) plus the primary colour.
        Box(
            Modifier.pressScale(press, .82f).clip(RoundedCornerShape(14.dp))
                .background(if (on) c.primarySoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center,
        ) { MIconView(tab.icon, tint) }
        Text(tab.label, color = tint, fontSize = 9.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, maxLines = 1)
    }
}
