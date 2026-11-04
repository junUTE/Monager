package com.monager.app.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.monager.app.ui.debts.DebtsViewModel
import com.monager.app.ui.components.Tone
import com.monager.app.ui.sample.TxItem
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
import java.time.LocalDate
import java.time.YearMonth

@Immutable
data class HomeState(
    val loaded: Boolean = false,
    val totalBalance: Long = 0,
    val monthIncome: Long = 0,
    val monthExpense: Long = 0,
    val budgetLimit: Long? = null,
    val budgetSpent: Long = 0,
    val recent: List<TxItem> = emptyList(),
)

private fun TxRow.toItem(today: LocalDate): TxItem {
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

class HomeViewModel(c: AppContainer) : ViewModel() {
    private val month = YearMonth.now()
    private val range = TimeRanges.month(month)

    val state: StateFlow<HomeState> = combine(
        c.transactions.totalBalance(),
        c.transactions.total(TxType.INCOME, range.from, range.to),
        c.transactions.total(TxType.EXPENSE, range.from, range.to),
        c.budgets.forMonth(TimeRanges.monthKey(month), range.from, range.to),
        c.transactions.recent(20),
    ) { balance, income, expense, budgets, recent ->
        val overall = budgets.firstOrNull { it.categoryId == null }
        val today = LocalDate.now()
        HomeState(true, balance, income, expense, overall?.limitAmount, overall?.spent ?: 0, recent.map { it.toItem(today) })
    }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default) // mapping off the main thread
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeState())
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
)

class BudgetsViewModel(private val c: AppContainer) : ViewModel() {
    private val month = YearMonth.now()
    private val range = TimeRanges.month(month)
    private val key = TimeRanges.monthKey(month)

    val state: StateFlow<BudgetsState> = combine(
        c.budgets.forMonth(key, range.from, range.to),
        c.categories.tree(TxType.EXPENSE),
        c.vaults.vaults(),
    ) { budgets, tree, vaults ->
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
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BudgetsState())

    fun setBudget(categoryId: Long?, limit: Long, onDone: () -> Unit) {
        viewModelScope.launch { c.budgets.set(key, categoryId, limit); onDone() }
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
        AddTransactionViewModel::class.java -> AddTransactionViewModel(container)
        CategoriesViewModel::class.java -> CategoriesViewModel(container)
        BudgetsViewModel::class.java -> BudgetsViewModel(container)
        DebtsViewModel::class.java -> DebtsViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    } as T
}
