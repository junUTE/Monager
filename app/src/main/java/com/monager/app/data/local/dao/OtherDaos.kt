package com.monager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.WalletEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name") fun observeAll(): Flow<List<CategoryEntity>>
    @Upsert suspend fun upsert(c: CategoryEntity): Long
}

@Dao
interface WalletDao {
    @Query("SELECT * FROM wallets WHERE archived = 0") fun observeActive(): Flow<List<WalletEntity>>
    @Upsert suspend fun upsert(w: WalletEntity): Long
}

@Dao
interface BudgetDao {
    @Query("SELECT * FROM budgets WHERE month = :month") fun observeMonth(month: Int): Flow<List<BudgetEntity>>
    @Upsert suspend fun upsert(b: BudgetEntity): Long
}

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts WHERE settled = 0 ORDER BY dueDate IS NULL, dueDate")
    fun observeOpen(): Flow<List<DebtEntity>>
    @Upsert suspend fun upsert(d: DebtEntity): Long
}
