package com.monager.app.ui

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.monager.app.core.TimeRanges
import com.monager.app.data.local.dao.TxRow
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity
import com.monager.app.data.repository.CategoryNode
import com.monager.app.di.AppContainer
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.Tone
import com.monager.app.ui.sample.TxItem
import kotlinx.coroutines.Dispatchers
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

class AppViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        HomeViewModel::class.java -> HomeViewModel(container)
        AddTransactionViewModel::class.java -> AddTransactionViewModel(container)
        else -> throw IllegalArgumentException("Unknown ViewModel ${modelClass.name}")
    } as T
}
