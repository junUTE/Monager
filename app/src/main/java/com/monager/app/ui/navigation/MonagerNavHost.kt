package com.monager.app.ui.navigation

import android.app.Activity
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.monager.app.ui.AddTransactionViewModel
import com.monager.app.ui.AppViewModelFactory
import com.monager.app.ui.HomeViewModel
import androidx.compose.runtime.mutableIntStateOf
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
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.debts.DebtsScreen
import com.monager.app.ui.home.HomeScreen
import com.monager.app.ui.theme.mc
import com.monager.app.ui.transaction.AddTransactionSheet

enum class Tab(val icon: MIcon, val label: String) {
    Home(MIcon.Home, "Trang chủ"), Stats(MIcon.Chart, "Báo cáo"), Budgets(MIcon.Wallet, "Hạn mức"), Debts(MIcon.Users, "Khoản nợ")
}

/** State-driven navigation: 4 tabs + Categories sub-screen + Add sheet. No NavHost overhead for such a small graph. */
@Composable
fun MonagerNavHost(container: AppContainer, dark: Boolean, onToggleDark: () -> Unit) {
    val c = mc
    val factory = remember(container) { AppViewModelFactory(container) }
    val homeVm: HomeViewModel = viewModel(factory = factory)
    val homeState by homeVm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableIntStateOf(Tab.Home.ordinal) }
    var inCategories by rememberSaveable { mutableStateOf(false) }
    var addType by rememberSaveable { mutableIntStateOf(-1) } // -1 = sheet closed

    val onHomeTop = tab == Tab.Home.ordinal && !inCategories
    // Light status-bar icons over the dark green home header, dark icons elsewhere in light theme.
    val view = LocalView.current
    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        val light = !(dark || onHomeTop)
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = light
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !dark
    }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        when {
            inCategories -> CategoriesScreen(onBack = { inCategories = false })
            tab == Tab.Home.ordinal -> HomeScreen(homeState, dark, onToggleDark, { inCategories = true }, { addType = it })
            tab == Tab.Stats.ordinal -> StatsScreen()
            tab == Tab.Budgets.ordinal -> BudgetsScreen()
            else -> DebtsScreen()
        }
        if (!inCategories) {
            BottomNav(
                selected = tab,
                onSelect = { tab = it },
                onAdd = { addType = 0 },
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        if (addType >= 0) {
            val addVm: AddTransactionViewModel = viewModel(factory = factory)
            AddTransactionSheet(addVm, initialType = addType, onClose = { addType = -1 })
        }
    }
}

@Composable
private fun BottomNav(selected: Int, onSelect: (Int) -> Unit, onAdd: () -> Unit, modifier: Modifier) {
    val c = mc
    Box(modifier.fillMaxWidth().background(c.surface.copy(alpha = .94f)).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            NavItem(Tab.Home, selected, onSelect, Modifier.weight(1f))
            NavItem(Tab.Stats, selected, onSelect, Modifier.weight(1f))
            Box(Modifier.weight(1f))
            NavItem(Tab.Budgets, selected, onSelect, Modifier.weight(1f))
            NavItem(Tab.Debts, selected, onSelect, Modifier.weight(1f))
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
    Column(
        modifier.height(64.dp).clickable(interactionSource = MutableInteractionSource(), indication = null) { onSelect(tab.ordinal) },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MIconView(tab.icon, tint)
        Text(tab.label, color = tint, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
