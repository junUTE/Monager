package com.monager.app.data.repository

import androidx.room.withTransaction
import com.monager.app.core.TimeRanges
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.local.dao.BudgetWithSpent
import com.monager.app.data.local.dao.DebtWithPaid
import com.monager.app.data.local.dao.TxRow
import com.monager.app.data.local.dao.VaultWithSaved
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.DebtPaymentEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

class TransactionRepository(private val db: MonagerDatabase) {
    private val dao = db.transactionDao()
    fun recent(limit: Int): Flow<List<TxRow>> = dao.observeRecent(limit)
    fun rowsBetween(from: Long, to: Long): Flow<List<TxRow>> = dao.observeRowsBetween(from, to)
    /** Balance as of the end of [today]; transactions dated later are "future" and not counted yet. */
    fun totalBalance(today: LocalDate = LocalDate.now()): Flow<Long> =
        dao.observeBalanceUntil(TimeRanges.startOfDay(today.plusDays(1)) - 1)
    fun total(type: TxType, from: Long, to: Long) = dao.observeTotal(type, from, to)
    fun totalsByParent(type: TxType, from: Long, to: Long) = dao.observeTotalsByParent(type, from, to)
    fun buckets(from: Long, to: Long, bucketMs: Long) = dao.observeBuckets(from, to, bucketMs)
    fun daily(from: Long, to: Long) = dao.observeDaily(from, to)

    /** Validates the invariants the UI relies on, then inserts. */
    suspend fun add(tx: TransactionEntity): Long {
        require(tx.amount > 0) { "amount must be positive" }
        when (tx.type) {
            TxType.EXPENSE, TxType.INCOME -> require(tx.categoryId != null) { "category required" }
            TxType.SAVING -> require(tx.vaultId != null) { "vault required" }
            TxType.TRANSFER -> require(tx.toWalletId != null && tx.toWalletId != tx.walletId) { "different target wallet required" }
        }
        return dao.insert(tx)
    }

    suspend fun delete(tx: TransactionEntity) = dao.delete(tx)
}

data class CategoryNode(val parent: CategoryEntity, val children: List<CategoryEntity>)

class CategoryRepository(private val db: MonagerDatabase) {
    private val dao = db.categoryDao()

    fun tree(type: TxType): Flow<List<CategoryNode>> = dao.observeByType(type).map { all ->
        val kids = all.filter { it.parentId != null }.groupBy { it.parentId }
        all.filter { it.parentId == null }.map { CategoryNode(it, kids[it.id].orEmpty()) }
    }

    suspend fun add(name: String, type: TxType, parentId: Long?, iconKey: String, toneKey: String): Long =
        db.withTransaction {
            val parent = parentId?.let { dao.get(it) }
            require(parent == null || parent.parentId == null) { "only two levels are allowed" }
            val effectiveType = parent?.type ?: type
            dao.insert(CategoryEntity(name = name.trim(), type = effectiveType, parentId = parentId, iconKey = iconKey, toneKey = toneKey, sortOrder = dao.maxSort(effectiveType, parentId) + 1))
        }

    suspend fun rename(c: CategoryEntity, name: String, iconKey: String, toneKey: String) =
        dao.update(c.copy(name = name.trim(), iconKey = iconKey, toneKey = toneKey))

    /** Persists a new order for siblings: list index becomes sortOrder. */
    suspend fun reorder(siblings: List<CategoryEntity>) =
        dao.updateAll(siblings.mapIndexed { i, c -> c.copy(sortOrder = i) })

    /** Blocks deletion of categories that have transactions (return false) so history is never orphaned. */
    suspend fun deleteIfUnused(id: Long): Boolean = db.withTransaction {
        if (dao.usageWithChildren(id) > 0) false else { dao.delete(id); true } // children cascade
    }
}

class WalletRepository(db: MonagerDatabase) {
    private val dao = db.walletDao()
    fun wallets(): Flow<List<WalletEntity>> = dao.observeActive()
    suspend fun save(w: WalletEntity) = dao.upsert(w)
}

class VaultRepository(db: MonagerDatabase) {
    private val dao = db.vaultDao()
    fun vaults(): Flow<List<VaultWithSaved>> = dao.observeWithSaved()

    /** Create (id == 0) or update. Vaults are not deletable yet: their SAVING transactions already left the wallet balance. */
    suspend fun save(v: VaultEntity): Long {
        require(v.name.isNotBlank() && v.target > 0)
        return dao.upsert(v.copy(name = v.name.trim()))
    }
}

class BudgetRepository(private val db: MonagerDatabase) {
    private val dao = db.budgetDao()
    fun forMonth(month: Int, from: Long, to: Long): Flow<List<BudgetWithSpent>> = dao.observeWithSpent(month, from, to)

    /** One budget per (category, month); categoryId == null is the overall budget. */
    suspend fun set(month: Int, categoryId: Long?, limit: Long) = db.withTransaction {
        require(limit > 0)
        val existing = dao.find(month, categoryId)
        dao.upsert(existing?.copy(limitAmount = limit) ?: BudgetEntity(categoryId = categoryId, month = month, limitAmount = limit))
    }

    suspend fun remove(id: Long) = dao.delete(id)
}

class DebtRepository(private val db: MonagerDatabase) {
    private val dao = db.debtDao()
    private val txDao = db.transactionDao()
    fun debts(iLent: Boolean): Flow<List<DebtWithPaid>> = dao.observeByDirection(iLent)
    fun payments(debtId: Long): Flow<List<DebtPaymentEntity>> = dao.observePayments(debtId)
    suspend fun add(d: DebtEntity): Long {
        require(d.personName.isNotBlank() && d.amount > 0)
        return dao.insert(d.copy(personName = d.personName.trim()))
    }

    /** Edits a debt; the total can never drop below what was already paid/collected. Returns false when rejected. */
    suspend fun edit(id: Long, name: String, amount: Long, dueDate: Long?, note: String): Boolean = db.withTransaction {
        val d = dao.get(id) ?: return@withTransaction false
        if (name.isBlank() || amount <= 0 || amount < dao.paidOf(id)) return@withTransaction false
        dao.update(d.copy(personName = name.trim(), amount = amount, dueDate = dueDate, note = note.trim()))
        true
    }

    /** Removes the debt and its payment history. Transactions created by past payments are kept (they were real cash movements). */
    suspend fun delete(id: Long) = dao.delete(id)

    /**
     * Partial pay/collect. Atomically: validates against the remaining balance, writes the payment row and,
     * when [walletId] is given, the matching INCOME (collecting) / EXPENSE (repaying) transaction.
     */
    suspend fun pay(debtId: Long, amount: Long, now: Long, walletId: Long?): Boolean = db.withTransaction {
        val debt = dao.get(debtId) ?: return@withTransaction false
        val remaining = debt.amount - dao.paidOf(debtId)
        if (amount <= 0 || amount > remaining) return@withTransaction false
        val txId = walletId?.let {
            txDao.insert(
                TransactionEntity(
                    walletId = it, type = if (debt.iLent) TxType.INCOME else TxType.EXPENSE, amount = amount,
                    note = (if (debt.iLent) "Thu nợ: " else "Trả nợ: ") + debt.personName, timestamp = now,
                ),
            )
        }
        dao.insertPayment(DebtPaymentEntity(debtId = debtId, amount = amount, timestamp = now, transactionId = txId))
        true
    }
}
