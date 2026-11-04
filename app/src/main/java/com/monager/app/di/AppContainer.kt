package com.monager.app.di

import android.content.ContentResolver
import android.content.Context
import com.monager.app.data.backup.BackupRepository
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.prefs.SettingsStore
import com.monager.app.data.repository.BudgetRepository
import com.monager.app.data.repository.CategoryRepository
import com.monager.app.data.repository.DebtRepository
import com.monager.app.data.repository.TransactionRepository
import com.monager.app.data.repository.VaultRepository
import com.monager.app.data.repository.WalletRepository
import java.io.File

/** Manual DI: no reflection, no startup cost. Everything is lazy. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: MonagerDatabase by lazy { MonagerDatabase.build(appContext) }
    val transactions by lazy { TransactionRepository(database) }
    val categories by lazy { CategoryRepository(database) }
    val wallets by lazy { WalletRepository(database) }
    val vaults by lazy { VaultRepository(database) }
    val budgets by lazy { BudgetRepository(database) }
    val debts by lazy { DebtRepository(database) }
    val backup by lazy { BackupRepository(database) }
    val settings by lazy { SettingsStore(appContext) }

    val contentResolver: ContentResolver get() = appContext.contentResolver
    fun databaseFile(): File = appContext.getDatabasePath("monager.db")
}
