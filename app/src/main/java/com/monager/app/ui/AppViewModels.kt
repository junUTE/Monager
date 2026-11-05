package com.monager.app.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.monager.app.core.MillisRange
import com.monager.app.core.TimeRanges
import com.monager.app.data.local.dao.TxRow
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity
import com.monager.app.data.repository.CategoryNode
import com.monager.app.di.AppContainer
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.analytics.AnalyticsViewModel
import com.monager.app.ui.debts.DebtsViewModel
import com.monager.app.ui.settings.SettingsViewModel
import com.monager.app.ui.components.Tone
import com.monager.app.ui.model.TxItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class TopSpend(val name: String, val icon: MIcon, val tone: Tone, val amount: Long)

/** Expense of one period (this week / this month), the previous period for the % change, and its top categories. */
@Immutable
data class PeriodSpend(val current: Long = 0, val previous: Long = 0, val top: List<TopSpend> = emptyList())

@Immutable
data class HomeState(
    val loaded: Boolean = false,
    val totalBalance: Long = 0,
    val monthIncome: Long = 0,
    val month: PeriodSpend = PeriodSpend(),
    val week: PeriodSpend = PeriodSpend(),
    /** Expense per day of the current month, normalised to 0..1 (precomputed so the chart only draws). */
    val dailyExpense: List<Float> = emptyList(),
    val recent: List<TxItem> = emptyList(),
)

internal fun TxRow.toItem(today: LocalDate): TxItem {
    val income = type == TxType.INCOME
    val title = categoryName ?: vaultName?.let { "Tiết kiệm" } ?: if (type == TxType.TRANSFER) "Chuyển khoản" else "Khác"
    val sub = note.ifBlank { parentName ?: vaultName.orEmpty() }
    return TxItem(
        id = id,
        icon = MIcon.fromKey(iconKey ?: if (type == TxType.TRANSFER) "Transfer" else "Users"),
        tone = Tone.fromKey(toneKey),
        title = title,
        note = sub,
        date = TimeRanges.label(timestamp, today),
        amount = if (income) amount else -amount,
    )
}

class HomeViewModel(private val c: AppContainer) : ViewModel() {
    private fun days(from: LocalDate, toExclusive: LocalDate) =
        MillisRange(TimeRanges.startOfDay(from), TimeRanges.startOfDay(toExclusive) - 1)

    private fun top(r: MillisRange): Flow<List<TopSpend>> =
        combine(c.transactions.totalsByParent(TxType.EXPENSE, r.from, r.to), c.categories.tree(TxType.EXPENSE)) { totals, tree ->
            val byId = tree.associateBy { it.parent.id }
            totals.mapNotNull { t ->
                byId[t.groupId]?.parent?.let { TopSpend(it.name, MIcon.fromKey(it.iconKey), Tone.fromKey(it.toneKey), t.total) }
            }.take(4)
        }

    private fun period(cur: MillisRange, prev: MillisRange): Flow<PeriodSpend> =
        combine(c.transactions.total(TxType.EXPENSE, cur.from, cur.to), c.transactions.total(TxType.EXPENSE, prev.from, prev.to), top(cur)) { a, b, t ->
            PeriodSpend(a, b, t)
        }

    private fun daily(month: YearMonth, r: MillisRange): Flow<List<Float>> = c.transactions.daily(r.from, r.to).map { rows ->
        val sums = LongArray(month.lengthOfMonth())
        rows.forEach { if (it.type == TxType.EXPENSE) sums[it.day.substring(8).toInt() - 1] += it.total }
        val max = sums.maxOrNull()?.takeIf { it > 0 }
        if (max == null) emptyList() else sums.map { it.toFloat() / max }
    }

    /** All windows (this week/month) are derived from today, so they roll over when the date changes while the app lives on. */
    private fun build(today: LocalDate): Flow<HomeState> {
        val month = YearMonth.from(today)
        val monthRange = TimeRanges.month(month)
        val weekStart = today.with(DayOfWeek.MONDAY)
        return combine(
            c.transactions.totalBalance(today),
            c.transactions.total(TxType.INCOME, monthRange.from, monthRange.to),
            combine(
                period(monthRange, TimeRanges.month(month.minusMonths(1))),
                period(days(weekStart, weekStart.plusDays(7)), days(weekStart.minusDays(7), weekStart)),
            ) { m, w -> m to w },
            daily(month, monthRange),
            c.transactions.recent(5),
        ) { balance, income, periods, dailyExpense, recent ->
            HomeState(true, balance, income, periods.first, periods.second, dailyExpense, recent.map { it.toItem(today) })
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeState> = c.today.flatMapLatest { build(it) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default) // mapping off the main thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
}

/** "Giao dịch" tab: one time window at a time (last month / this month / future), grouped by day. */
@Immutable
data class TxGroup(val label: String, val items: List<TxItem>)

@Immutable
data class TransactionsState(val loaded: Boolean = false, val balance: Long = 0, val groups: List<TxGroup> = emptyList())

class TransactionsViewModel(private val c: AppContainer) : ViewModel() {
    private val tab = MutableStateFlow(1)
    val selected: StateFlow<Int> = tab

    fun select(i: Int) { tab.value = i }

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<TransactionsState> = combine(tab, c.today) { t, today -> t to today }.flatMapLatest { (t, today) ->
        val thisMonth = YearMonth.from(today)
        val r = when (t) {
            0 -> TimeRanges.month(thisMonth.minusMonths(1))
            1 -> TimeRanges.month(thisMonth)
            // "Future": anything after today (planned transactions, not yet in the balance).
            else -> MillisRange(TimeRanges.startOfDay(today.plusDays(1)), Long.MAX_VALUE)
        }
        combine(c.transactions.totalBalance(today), c.transactions.rowsBetween(r.from, r.to)) { balance, rows ->
            val groups = ArrayList<TxGroup>()
            var label = ""
            var bucket = ArrayList<TxItem>()
            rows.forEach { row ->
                val item = row.toItem(today)
                if (item.date != label && bucket.isNotEmpty()) { groups += TxGroup(label, bucket); bucket = ArrayList() }
                label = item.date
                bucket += item
            }
            if (bucket.isNotEmpty()) groups += TxGroup(label, bucket)
            TransactionsState(true, balance, groups)
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TransactionsState())
}

class AddTransactionViewModel(private val c: AppContainer) : ViewModel() {
    val wallets: StateFlow<List<WalletEntity>> =
        c.wallets.wallets().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val vaults: StateFlow<List<VaultEntity>> =
        c.vaults.vaults().map { list -> list.map { it.vault } }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun tree(type: TxType): Flow<List<CategoryNode>> = c.categories.tree(type)

    fun save(tx: TransactionEntity, onDone: () -> Unit) {
        viewModelScope.launch {
            c.transactions.add(tx)
            onDone()
        }
    }
}

class CategoriesViewModel(private val c: AppContainer) : ViewModel() {
    private val type = MutableStateFlow(TxType.EXPENSE)

    @OptIn(ExperimentalCoroutinesApi::class)
    val tree: StateFlow<List<CategoryNode>> = type.flatMapLatest { c.categories.tree(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setType(t: TxType) { type.value = t }

    /** id == 0 creates (as subcategory when [parentId] != null); otherwise updates name/icon/tone. */
    fun save(id: Long, parentId: Long?, name: String, iconKey: String, toneKey: String, onDone: () -> Unit) {
        viewModelScope.launch {
            if (id == 0L) c.categories.add(name, type.value, parentId, iconKey, toneKey)
            else tree.value.flatMap { listOf(it.parent) + it.children }.firstOrNull { it.id == id }
                ?.let { c.categories.rename(it, name, iconKey, toneKey) }
            onDone()
        }
    }

    /** Result false = category (or a child) still has transactions, so it was kept. */
    fun delete(id: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(c.categories.deleteIfUnused(id)) }
    }

    /** Swaps a category with its previous (delta = -1) or next (delta = +1) sibling and persists the order. */
    fun move(id: Long, delta: Int) {
        val nodes = tree.value
        val siblings = nodes.firstOrNull { n -> n.children.any { it.id == id } }?.children ?: nodes.map { it.parent }
        val i = siblings.indexOfFirst { it.id == id }
        val j = i + delta
        if (i < 0 || j !in siblings.indices) return
        val reordered = siblings.toMutableList().also { it[i] = siblings[j]; it[j] = siblings[i] }
        viewModelScope.launch { c.categories.reorder(reordered) }
    }
}

@Immutable
data class BudgetRow(val id: Long, val categoryId: Long?, val name: String, val icon: MIcon, val tone: Tone, val limit: Long, val spent: Long) {
    val pct: Int get() = if (limit <= 0) 0 else (spent * 100 / limit).toInt()
}

@Immutable
data class VaultRow(val id: Long, val name: String, val iconKey: String, val target: Long, val saved: Long) {
    val pct: Int get() = if (target <= 0) 0 else (saved * 100 / target).toInt().coerceAtMost(100)
}

@Immutable
data class BudgetsState(
    val overall: BudgetRow? = null,
    val rows: List<BudgetRow> = emptyList(),
    val vaults: List<VaultRow> = emptyList(),
    /** Expense parents that do not have a budget yet this month (choices for "add budget"). */
    val available: List<CategoryEntity> = emptyList(),
    /** Number of budgets set last month; > 0 with none this month offers "reuse last month". */
    val previousCount: Int = 0,
)

class BudgetsViewModel(private val c: AppContainer) : ViewModel() {
    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<BudgetsState> = c.today.flatMapLatest { today ->
        val month = YearMonth.from(today)
        val range = TimeRanges.month(month)
        combine(
            c.budgets.forMonth(TimeRanges.monthKey(month), range.from, range.to),
            c.categories.tree(TxType.EXPENSE),
            c.vaults.vaults(),
            c.budgets.count(TimeRanges.monthKey(month.minusMonths(1))),
        ) { budgets, tree, vaults, previousCount ->
            val byId = tree.associateBy { it.parent.id }
            val rows = budgets.filter { it.categoryId != null }.mapNotNull { b ->
                val cat = byId[b.categoryId]?.parent ?: return@mapNotNull null
                BudgetRow(b.id, cat.id, cat.name, MIcon.fromKey(cat.iconKey), Tone.fromKey(cat.toneKey), b.limitAmount, b.spent)
            }
            val overall = budgets.firstOrNull { it.categoryId == null }
                ?.let { BudgetRow(it.id, null, "Tổng tháng", MIcon.Wallet, Tone.Sage, it.limitAmount, it.spent) }
            val used = rows.mapNotNull { it.categoryId }.toSet()
            BudgetsState(
                overall, rows,
                vaults.map { VaultRow(it.vault.id, it.vault.name, it.vault.iconKey, it.vault.target, it.saved) },
                tree.map { it.parent }.filter { it.id !in used },
                previousCount,
            )
        }
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsState())

    fun setBudget(categoryId: Long?, limit: Long, onDone: () -> Unit) {
        viewModelScope.launch { c.budgets.set(TimeRanges.monthKey(YearMonth.from(c.today.value)), categoryId, limit); onDone() }
    }

    fun copyLastMonth(onDone: () -> Unit) {
        val month = YearMonth.from(c.today.value)
        viewModelScope.launch { c.budgets.copyMonth(TimeRanges.monthKey(month.minusMonths(1)), TimeRanges.monthKey(month)); onDone() }
    }

    fun deleteVault(id: Long, onDone: () -> Unit) {
        viewModelScope.launch { c.vaults.delete(id); onDone() }
    }

    fun removeBudget(id: Long, onDone: () -> Unit) {
        viewModelScope.launch { c.budgets.remove(id); onDone() }
    }

    fun saveVault(id: Long, name: String, target: Long, onDone: () -> Unit) {
        viewModelScope.launch { c.vaults.save(VaultEntity(id = id, name = name, target = target)); onDone() }
    }
}

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(container)
        TransactionsViewModel::class.java -> TransactionsViewModel(container)
        AddTransactionViewModel::class.java -> AddTransactionViewModel(container)
        CategoriesViewModel::class.java -> CategoriesViewModel(container)
        BudgetsViewModel::class.java -> BudgetsViewModel(container)
        DebtsViewModel::class.java -> DebtsViewModel(container)
        AnalyticsViewModel::class.java -> AnalyticsViewModel(container)
        SettingsViewModel::class.java -> SettingsViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    } as T
}
