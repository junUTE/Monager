package com.monager.app.ui.debts

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monager.app.core.DebtMath
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.DebtPaymentEntity
import com.monager.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@Immutable
data class DebtRow(
    val id: Long,
    val name: String,
    val initials: String,
    val total: Long,
    val paid: Long,
    val dueMillis: Long?,
    val note: String,
    val overdueDays: Int,
) {
    val remaining: Long get() = total - paid
    val settled: Boolean get() = remaining <= 0
}

@Immutable
data class DebtsState(val rows: List<DebtRow> = emptyList(), val totalRemaining: Long = 0, val openCount: Int = 0)

class DebtsViewModel(private val c: AppContainer) : ViewModel() {
    private val lend = MutableStateFlow(true)

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<DebtsState> = lend.flatMapLatest { iLent ->
        c.debts.debts(iLent).map { list ->
            val today = LocalDate.now()
            val rows = list.map {
                val remaining = it.debt.amount - it.paid
                DebtRow(
                    it.debt.id, it.debt.personName, DebtMath.initials(it.debt.personName), it.debt.amount, it.paid,
                    it.debt.dueDate, it.debt.note, DebtMath.daysOverdue(it.debt.dueDate, remaining, today),
                )
            }.sortedBy { it.settled } // settled debts sink to the bottom (stable sort keeps due-date order)
            DebtsState(rows, rows.sumOf { it.remaining.coerceAtLeast(0) }, rows.count { !it.settled })
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DebtsState())

    fun setLend(value: Boolean) { lend.value = value }

    fun payments(debtId: Long): Flow<List<DebtPaymentEntity>> = c.debts.payments(debtId)

    fun add(name: String, amount: Long, due: Long?, note: String, onDone: () -> Unit) {
        viewModelScope.launch {
            c.debts.add(DebtEntity(personName = name, amount = amount, iLent = lend.value, dueDate = due, note = note.trim(), createdAt = System.currentTimeMillis()))
            onDone()
        }
    }

    /** onResult(false) when the new total would drop below what was already paid. */
    fun edit(id: Long, name: String, amount: Long, due: Long?, note: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch { onResult(c.debts.edit(id, name, amount, due, note)) }
    }

    fun delete(id: Long, onDone: () -> Unit) { viewModelScope.launch { c.debts.delete(id); onDone() } }

    /** [record] true also writes the matching income/expense transaction into the first wallet. */
    fun pay(debtId: Long, amount: Long, record: Boolean, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val walletId = if (record) c.wallets.wallets().first().firstOrNull()?.id else null
            onResult(c.debts.pay(debtId, amount, System.currentTimeMillis(), walletId))
        }
    }
}
