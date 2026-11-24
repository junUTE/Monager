package com.monager.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.DebtPaymentEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity

data class ExportRow(
    val timestamp: Long,
    val type: TxType,
    val amount: Long,
    val categoryName: String?,
    val parentName: String?,
    val walletName: String,
    val toWalletName: String?,
    val vaultName: String?,
    val note: String,
)

/** A budget with readable names: [topName] null = overall monthly budget; [subName] set when the budget is on a subcategory. */
data class BudgetExportRow(val month: Int, val limitAmount: Long, val topName: String?, val subName: String?)

data class TableCounts(val transactions: Int, val categories: Int, val wallets: Int, val debts: Int)

/** Bulk read/replace access used only by backup, restore and import/export. */
@Dao
interface BackupDao {
    @Query("SELECT * FROM wallets ORDER BY id") suspend fun wallets(): List<WalletEntity>
    @Query("SELECT * FROM categories ORDER BY id") suspend fun categories(): List<CategoryEntity>
    @Query("SELECT * FROM vaults ORDER BY id") suspend fun vaults(): List<VaultEntity>
    @Query("SELECT * FROM transactions ORDER BY id") suspend fun transactions(): List<TransactionEntity>
    @Query("SELECT * FROM budgets ORDER BY id") suspend fun budgets(): List<BudgetEntity>
    @Query("SELECT * FROM debts ORDER BY id") suspend fun debts(): List<DebtEntity>
    @Query("SELECT * FROM debt_payments ORDER BY id") suspend fun debtPayments(): List<DebtPaymentEntity>

    @Query("DELETE FROM debt_payments") suspend fun clearDebtPayments()
    @Query("DELETE FROM debts") suspend fun clearDebts()
    @Query("DELETE FROM budgets") suspend fun clearBudgets()
    @Query("DELETE FROM transactions") suspend fun clearTransactions()
    @Query("DELETE FROM vaults") suspend fun clearVaults()
    @Query("DELETE FROM categories") suspend fun clearCategories()
    @Query("DELETE FROM wallets") suspend fun clearWallets()

    @Insert suspend fun insertWallets(l: List<WalletEntity>)
    @Insert suspend fun insertCategories(l: List<CategoryEntity>)
    @Insert suspend fun insertVaults(l: List<VaultEntity>)
    @Insert suspend fun insertTransactions(l: List<TransactionEntity>)
    @Insert suspend fun insertBudgets(l: List<BudgetEntity>)
    @Insert suspend fun insertDebts(l: List<DebtEntity>)
    @Insert suspend fun insertDebtPayments(l: List<DebtPaymentEntity>)

    @Query(
        """
        SELECT t.timestamp, t.type, t.amount,
               CASE WHEN p.name IS NOT NULL THEN p.name ELSE c.name END AS categoryName,
               CASE WHEN p.name IS NOT NULL THEN c.name ELSE NULL END AS parentName,
               w.name AS walletName, w2.name AS toWalletName, v.name AS vaultName, t.note
        FROM transactions t
        JOIN wallets w ON w.id = t.walletId
        LEFT JOIN wallets w2 ON w2.id = t.toWalletId
        LEFT JOIN categories c ON c.id = t.categoryId
        LEFT JOIN categories p ON p.id = c.parentId
        LEFT JOIN vaults v ON v.id = t.vaultId
        ORDER BY t.timestamp, t.id
        """,
    )
    suspend fun exportRows(): List<ExportRow>

    @Query(
        """
        SELECT b.month, b.limitAmount,
               CASE WHEN p.name IS NOT NULL THEN p.name ELSE c.name END AS topName,
               CASE WHEN p.name IS NOT NULL THEN c.name ELSE NULL END AS subName
        FROM budgets b
        LEFT JOIN categories c ON c.id = b.categoryId
        LEFT JOIN categories p ON p.id = c.parentId
        ORDER BY b.month, b.categoryId IS NOT NULL, topName, subName
        """,
    )
    suspend fun budgetExportRows(): List<BudgetExportRow>

    /** All savings goals, archived ones too, with the amount saved so far. */
    @Query(
        """
        SELECT v.*, COALESCE((SELECT SUM(t.amount) FROM transactions t WHERE t.vaultId = v.id AND t.type = 'SAVING'), 0) AS saved
        FROM vaults v ORDER BY v.id
        """,
    )
    suspend fun vaultExportRows(): List<VaultWithSaved>

    /** Number of existing transactions identical to the given one within the time window [from, to) (exports keep minute precision); used to skip duplicates when importing a file twice. */
    @Query(
        """
        SELECT COUNT(*) FROM transactions
        WHERE timestamp >= :from AND timestamp < :to AND amount = :amount AND type = :type AND walletId = :walletId AND note = :note
          AND categoryId IS :categoryId AND vaultId IS :vaultId AND toWalletId IS :toWalletId
        """,
    )
    suspend fun countSame(from: Long, to: Long, amount: Long, type: TxType, walletId: Long, note: String, categoryId: Long?, vaultId: Long?, toWalletId: Long?): Int

    @Query("SELECT (SELECT COUNT(*) FROM transactions) AS transactions, (SELECT COUNT(*) FROM categories) AS categories, (SELECT COUNT(*) FROM wallets) AS wallets, (SELECT COUNT(*) FROM debts) AS debts")
    suspend fun counts(): TableCounts
}
