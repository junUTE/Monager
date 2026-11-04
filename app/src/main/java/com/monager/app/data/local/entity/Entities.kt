package com.monager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class TxType { EXPENSE, INCOME }

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialBalance: Long = 0,
    val colorArgb: Int,
    val iconKey: String,
    val archived: Boolean = false,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType,
    val colorArgb: Int,
    val iconKey: String,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(WalletEntity::class, ["id"], ["walletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("timestamp"),
        Index("categoryId"),
        Index("walletId", "timestamp"),
        Index("categoryId", "timestamp"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val categoryId: Long,
    val type: TxType,
    val amount: Long,
    val note: String = "",
    val timestamp: Long,
)

@Entity(
    tableName = "budgets",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId", "month", unique = true)],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val month: Int, // yyyyMM
    val limitAmount: Long,
)

@Entity(tableName = "debts", indices = [Index("settled", "dueDate")])
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Long,
    val iLent: Boolean, // true = they owe me
    val dueDate: Long? = null,
    val settled: Boolean = false,
    val note: String = "",
)
