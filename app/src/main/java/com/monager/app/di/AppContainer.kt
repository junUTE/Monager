package com.monager.app.di

import android.content.Context
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.repository.TransactionRepository

/** Manual DI: no reflection, no startup cost. Everything is lazy. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: MonagerDatabase by lazy { MonagerDatabase.build(appContext) }
    val transactionRepository: TransactionRepository by lazy {
        TransactionRepository(database.transactionDao())
    }
}
