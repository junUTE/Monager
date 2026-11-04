package com.monager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.monager.app.data.local.dao.BudgetDao
import com.monager.app.data.local.dao.CategoryDao
import com.monager.app.data.local.dao.DebtDao
import com.monager.app.data.local.dao.TransactionDao
import com.monager.app.data.local.dao.WalletDao
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.Converters
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class, CategoryEntity::class, TransactionEntity::class,
        BudgetEntity::class, DebtEntity::class,
    ],
    version = 1,
    exportSchema = true, // app/schemas is committed -> enables migration tests
)
@TypeConverters(Converters::class)
abstract class MonagerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun budgetDao(): BudgetDao
    abstract fun debtDao(): DebtDao

    companion object {
        fun build(context: Context): MonagerDatabase =
            Room.databaseBuilder(context, MonagerDatabase::class.java, "monager.db")
                // Never fallbackToDestructiveMigration: user finance data must survive updates.
                .build()
    }
}
