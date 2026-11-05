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

/** Row for lists: joined once in SQL so the UI never does per-item lookups. */
data class TxRow(
    val id: Long,
    val type: TxType,
    val amount: Long,
    val note: String,
    val timestamp: Long,
    val categoryName: String?,
    val parentName: String?,
    val iconKey: String?,
    val toneKey: String?,
    val vaultName: String?,
)

data class GroupTotal(val groupId: Long, val total: Long)
data class DayTotal(val day: String, val type: TxType, val total: Long)
data class BucketTotal(val bucket: Int, val type: TxType, val total: Long)

private const val ROW_SELECT = """
    SELECT t.id, t.type, t.amount, t.note, t.timestamp,
           c.name AS categoryName, p.name AS parentName,
           COALESCE(c.iconKey, v.iconKey) AS iconKey, c.toneKey AS toneKey, v.name AS vaultName
    FROM transactions t
    LEFT JOIN categories c ON c.id = t.categoryId
    LEFT JOIN categories p ON p.id = c.parentId
    LEFT JOIN vaults v ON v.id = t.vaultId
"""

@Dao
interface TransactionDao {
    @Insert suspend fun insert(tx: TransactionEntity): Long
    @Update suspend fun update(tx: TransactionEntity)
    @Delete suspend fun delete(tx: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun get(id: Long): TransactionEntity?

    @Query("$ROW_SELECT ORDER BY t.timestamp DESC, t.id DESC")
    fun pagedRows(): PagingSource<Int, TxRow>

    @Query("$ROW_SELECT ORDER BY t.timestamp DESC, t.id DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TxRow>>

    /** One time window (a month, or "future"); windowed so the screen never loads the whole history. */
    @Query("$ROW_SELECT WHERE t.timestamp BETWEEN :from AND :to ORDER BY t.timestamp DESC, t.id DESC LIMIT 1000")
    fun observeRowsBetween(from: Long, to: Long): Flow<List<TxRow>>

    /** Income minus expense minus savings across active wallets; transfers net to zero. */
    @Query(
        """
        SELECT COALESCE((SELECT SUM(initialBalance) FROM wallets WHERE archived = 0), 0) +
               COALESCE((SELECT SUM(CASE type WHEN 'INCOME' THEN amount WHEN 'EXPENSE' THEN -amount WHEN 'SAVING' THEN -amount ELSE 0 END)
                         FROM transactions), 0)
        """,
    )
    fun observeTotalBalance(): Flow<Long>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM transactions WHERE type = :type AND timestamp BETWEEN :from AND :to")
    fun observeTotal(type: TxType, from: Long, to: Long): Flow<Long>

    /** Totals grouped to the PARENT category (children roll up), largest first. */
    @Query(
        """
        SELECT COALESCE(c.parentId, c.id) AS groupId, SUM(t.amount) AS total
        FROM transactions t JOIN categories c ON c.id = t.categoryId
        WHERE t.type = :type AND t.timestamp BETWEEN :from AND :to
        GROUP BY groupId ORDER BY total DESC
        """,
    )
    fun observeTotalsByParent(type: TxType, from: Long, to: Long): Flow<List<GroupTotal>>

    /** Time buckets for the cash-flow chart; bucket = (timestamp - from) / bucketMs. */
    @Query(
        """
        SELECT CAST((timestamp - :from) / :bucketMs AS INTEGER) AS bucket, type, SUM(amount) AS total
        FROM transactions
        WHERE type IN ('INCOME', 'EXPENSE') AND timestamp BETWEEN :from AND :to
        GROUP BY bucket, type
        """,
    )
    fun observeBuckets(from: Long, to: Long, bucketMs: Long): Flow<List<BucketTotal>>

    /** Per local-calendar-day totals (at most ~366 x 2 rows for a year); the caller folds them into week/month buckets. */
    @Query(
        """
        SELECT date(timestamp / 1000, 'unixepoch', 'localtime') AS day, type, SUM(amount) AS total
        FROM transactions
        WHERE type IN ('INCOME', 'EXPENSE') AND timestamp BETWEEN :from AND :to
        GROUP BY day, type
        """,
    )
    fun observeDaily(from: Long, to: Long): Flow<List<DayTotal>>
}
