package com.monager.app.di

import android.content.Context
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.repository.BudgetRepository
import com.monager.app.data.repository.CategoryRepository
import com.monager.app.data.repository.DebtRepository
import com.monager.app.data.repository.TransactionRepository
import com.monager.app.data.repository.VaultRepository
import com.monager.app.data.repository.WalletRepository

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
}
