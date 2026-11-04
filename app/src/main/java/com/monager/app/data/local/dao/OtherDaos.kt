package com.monager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.DebtPaymentEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    /** Flat list; CategoryRepository assembles the parent/child tree. */
    @Query("SELECT * FROM categories WHERE type = :type ORDER BY sortOrder, id")
    fun observeByType(type: TxType): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id") suspend fun get(id: Long): CategoryEntity?
    @Query("SELECT COALESCE(MAX(sortOrder), 0) FROM categories WHERE type = :type AND parentId IS :parentId")
    suspend fun maxSort(type: TxType, parentId: Long?): Int
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id") suspend fun usageCount(id: Long): Int
    @Query("SELECT COUNT(*) FROM categories WHERE parentId = :id") suspend fun childCount(id: Long): Int
    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :id OR categoryId IN (SELECT id FROM categories WHERE parentId = :id)")
    suspend fun usageWithChildren(id: Long): Int
    @Insert suspend fun insert(c: CategoryEntity): Long
    @Update suspend fun update(c: CategoryEntity)
    @Update suspend fun updateAll(list: List<CategoryEntity>)
    @Query("DELETE FROM categories WHERE id = :id") suspend fun delete(id: Long)
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE archived = 0 ORDER BY sortOrder, id") fun observeActive(): Flow<List<WalletEntity>>
    @Upsert suspend fun upsert(w: WalletEntity): Long
}

data class BudgetWithSpent(val id: Long, val categoryId: Long?, val limitAmount: Long, val spent: Long)

@Dao
interface BudgetDao {
    @Query(
        """
        SELECT b.id, b.categoryId, b.limitAmount,
          COALESCE((SELECT SUM(t.amount) FROM transactions t
                    WHERE t.type = 'EXPENSE' AND t.timestamp BETWEEN :from AND :to
                      AND (b.categoryId IS NULL OR t.categoryId = b.categoryId
                           OR t.categoryId IN (SELECT id FROM categories WHERE parentId = b.categoryId))), 0) AS spent
        FROM budgets b WHERE b.month = :month ORDER BY b.categoryId IS NOT NULL, b.id
        """,
    )
    fun observeWithSpent(month: Int, from: Long, to: Long): Flow<List<BudgetWithSpent>>

    @Query("SELECT * FROM budgets WHERE month = :month AND categoryId IS :categoryId LIMIT 1")
    suspend fun find(month: Int, categoryId: Long?): BudgetEntity?
    @Upsert suspend fun upsert(b: BudgetEntity): Long
    @Query("DELETE FROM budgets WHERE id = :id") suspend fun delete(id: Long)
}

data class VaultWithSaved(@Embedded val vault: VaultEntity, val saved: Long)

@Dao
interface VaultDao {
    @Query(
        """
        SELECT v.*, COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.vaultId = v.id AND t.type = 'SAVING'), 0) AS saved
        FROM vaults v WHERE v.archived = 0 ORDER BY v.id
        """,
    )
    fun observeWithSaved(): Flow<List<VaultWithSaved>>
    @Upsert suspend fun upsert(v: VaultEntity): Long
}

data class DebtWithPaid(@Embedded val debt: DebtEntity, val paid: Long)

@Dao
interface DebtDao {
    @Query(
        """
        SELECT d.*, COALESCE((SELECT SUM(p.amount) FROM debt_payments p WHERE p.debtId = d.id), 0) AS paid
        FROM debts d WHERE d.iLent = :iLent ORDER BY d.dueDate IS NULL, d.dueDate, d.id
        """,
    )
    fun observeByDirection(iLent: Boolean): Flow<List<DebtWithPaid>>

    @Query("SELECT * FROM debts WHERE id = :id") suspend fun get(id: Long): DebtEntity?
    @Query("SELECT COALESCE(SUM(amount), 0) FROM debt_payments WHERE debtId = :id") suspend fun paidOf(id: Long): Long
    @Insert suspend fun insert(d: DebtEntity): Long
    @Update suspend fun update(d: DebtEntity)
    @Query("DELETE FROM debts WHERE id = :id") suspend fun delete(id: Long)
    @Insert suspend fun insertPayment(p: DebtPaymentEntity): Long
    @Query("SELECT * FROM debt_payments WHERE debtId = :id ORDER BY timestamp DESC, id DESC")
    fun observePayments(id: Long): Flow<List<DebtPaymentEntity>>
}
