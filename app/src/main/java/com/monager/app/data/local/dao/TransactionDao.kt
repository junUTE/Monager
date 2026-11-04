package com.monager.app.data.local.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import kotlinx.coroutines.flow.Flow

data class CategoryTotal(val categoryId: Long, val total: Long)

@Dao
interface TransactionDao {
    @Insert suspend fun insert(tx: TransactionEntity): Long
    @Update suspend fun update(tx: TransactionEntity)
    @Delete suspend fun delete(tx: TransactionEntity)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun pagedAll(): PagingSource<Int, TransactionEntity>

    /** Aggregate in SQL and return tiny result sets. */
    @Query("SELECT COALESCE(SUM(amount),0) FROM transactions WHERE type = :type AND timestamp BETWEEN :from AND :to")
    fun observeTotal(type: TxType, from: Long, to: Long): Flow<Long>

    @Query(
        "SELECT categoryId, SUM(amount) AS total FROM transactions " +
            "WHERE type = :type AND timestamp BETWEEN :from AND :to GROUP BY categoryId ORDER BY total DESC"
    )
    fun observeTotalsByCategory(type: TxType, from: Long, to: Long): Flow<List<CategoryTotal>>
}
