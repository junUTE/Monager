package com.monager.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** EXPENSE/INCOME are also category kinds; SAVING deposits into a vault; TRANSFER moves between wallets. */
enum class TxType { EXPENSE, INCOME, SAVING, TRANSFER }

@Entity(tableName = "wallets")
data class WalletEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val initialBalance: Long = 0,
    val iconKey: String = "Wallet",
    val archived: Boolean = false,
    val sortOrder: Int = 0,
)

/** Two-level tree: parentId == null -> parent category, otherwise subcategory. */
@Entity(
    tableName = "categories",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["parentId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("parentId"), Index("type", "sortOrder")],
)
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val type: TxType, // EXPENSE or INCOME only
    val parentId: Long? = null,
    val iconKey: String,
    val toneKey: String,
    val sortOrder: Int = 0,
)

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val target: Long,
    val iconKey: String = "Sparkles",
    val deadline: Long? = null,
    val archived: Boolean = false,
)

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(WalletEntity::class, ["id"], ["walletId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(VaultEntity::class, ["id"], ["vaultId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(WalletEntity::class, ["id"], ["toWalletId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [
        Index("timestamp"),
        Index("walletId", "timestamp"),
        Index("categoryId", "timestamp"),
        Index("vaultId"),
        Index("toWalletId"),
        Index("type", "timestamp"),
    ],
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walletId: Long,
    val type: TxType,
    val amount: Long, // always > 0, minor units
    val categoryId: Long? = null, // EXPENSE / INCOME
    val vaultId: Long? = null, // SAVING
    val toWalletId: Long? = null, // TRANSFER
    val note: String = "",
    val timestamp: Long,
)

/** categoryId == null -> overall monthly budget. Uniqueness for the null case is enforced in BudgetRepository. */
@Entity(
    tableName = "budgets",
    foreignKeys = [ForeignKey(CategoryEntity::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId", "month"), Index("month")],
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long? = null,
    val month: Int, // yyyyMM
    val limitAmount: Long,
)

@Entity(tableName = "debts", indices = [Index("iLent", "dueDate")])
data class DebtEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val personName: String,
    val amount: Long,
    val iLent: Boolean, // true = they owe me, false = I owe them
    val dueDate: Long? = null,
    val note: String = "",
    val createdAt: Long,
)

@Entity(
    tableName = "debt_payments",
    foreignKeys = [ForeignKey(DebtEntity::class, ["id"], ["debtId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("debtId")],
)
data class DebtPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val debtId: Long,
    val amount: Long,
    val timestamp: Long,
    val transactionId: Long? = null,
)
