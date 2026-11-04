package com.monager.app.data.repository

import com.monager.app.data.local.dao.TransactionDao
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType

class TransactionRepository(private val dao: TransactionDao) {
    fun pagedAll() = dao.pagedAll()
    fun total(type: TxType, from: Long, to: Long) = dao.observeTotal(type, from, to)
    fun totalsByCategory(type: TxType, from: Long, to: Long) = dao.observeTotalsByCategory(type, from, to)
    suspend fun add(tx: TransactionEntity) = dao.insert(tx)
}
