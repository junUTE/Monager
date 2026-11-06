package com.monager.app.data.backup

import androidx.room.withTransaction
import com.monager.app.data.local.DefaultData
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity
import com.monager.app.data.backup.TransactionTable.cell
import com.monager.app.data.backup.TransactionTable.normalize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.InputStream
import java.io.OutputStream

enum class FileKind { JSON, XLSX, CSV }

data class ImportResult(val imported: Int, val duplicates: Int, val errors: Int, val firstErrors: List<String>, val createdCategories: Int)
data class RestoreResult(val transactions: Int, val categories: Int, val wallets: Int, val debts: Int)

class BackupRepository(private val db: MonagerDatabase) {
    private val dao = db.backupDao()

    // ---------------- export ----------------

    suspend fun exportJson(out: OutputStream) = withContext(Dispatchers.IO) {
        val snapshot = db.withTransaction {
            Snapshot(dao.wallets(), dao.categories(), dao.vaults(), dao.transactions(), dao.budgets(), dao.debts(), dao.debtPayments())
        }
        JsonBackup.write(snapshot, System.currentTimeMillis(), out)
    }

    suspend fun exportCsv(out: OutputStream) = withContext(Dispatchers.IO) {
        val rows = TransactionTable.toRows(dao.exportRows(), neutralize = true)
        val w = out.bufferedWriter(Charsets.UTF_8)
        CsvCodec.write(rows, w)
        w.flush()
    }

    suspend fun exportXlsx(out: OutputStream) = withContext(Dispatchers.IO) {
        val rows = TransactionTable.toRows(dao.exportRows(), neutralize = false)
        XlsxCodec.write(rows, setOf(TransactionTable.AMOUNT_COLUMN), "Transactions", out)
    }

    // ---------------- import ----------------

    /** Looks at the first bytes: "PK" = xlsx (zip), "{" = JSON backup, anything else is treated as CSV. */
    fun detect(input: BufferedInputStream): FileKind {
        input.mark(8)
        val head = ByteArray(8)
        val n = input.read(head)
        input.reset()
        if (n >= 2 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()) return FileKind.XLSX
        val text = String(head, 0, n.coerceAtLeast(0), Charsets.UTF_8).removePrefix("﻿").trimStart()
        return if (text.startsWith("{")) FileKind.JSON else FileKind.CSV
    }

    suspend fun readTable(kind: FileKind, input: InputStream): List<List<String>> = withContext(Dispatchers.IO) {
        when (kind) {
            FileKind.XLSX -> XlsxCodec.read(input)
            FileKind.CSV -> CsvCodec.parse(input.readBytes().toString(Charsets.UTF_8))
            FileKind.JSON -> throw IllegalArgumentException("JSON is not a table")
        }
    }

    private data class Pending(val ts: Long, val type: TxType, val amount: Long, val walletId: Long, val categoryId: Long?, val vaultId: Long?, val toWalletId: Long?, val note: String)

    /**
     * Appends transactions from a table. Unknown categories, wallets and savings vaults are created.
     * Rows that already exist (same minute, amount, type, wallet, category, note) are skipped, so importing
     * the same file twice does not double your data. Everything happens in one DB transaction.
     */
    suspend fun importTable(rows: List<List<String>>): ImportResult = withContext(Dispatchers.IO) {
        db.withTransaction {
            if (rows.isEmpty()) return@withTransaction ImportResult(0, 0, 0, emptyList(), 0)
            val (cols, hasHeader) = TransactionTable.detectColumns(rows.first())
            val catDao = db.categoryDao()

            val wallets = dao.wallets().associateBy { normalize(it.name) }.mapValues { it.value.id }.toMutableMap()
            val vaults = dao.vaults().associateBy { normalize(it.name) }.mapValues { it.value.id }.toMutableMap()
            val parents = HashMap<Pair<TxType, String>, Long>()
            val children = HashMap<Pair<Long, String>, Long>()
            dao.categories().forEach { c ->
                if (c.parentId == null) parents.putIfAbsent(c.type to normalize(c.name), c.id) else children.putIfAbsent(c.parentId to normalize(c.name), c.id)
            }
            var created = 0
            if (wallets.isEmpty()) wallets[normalize("Cash")] = db.walletDao().upsert(WalletEntity(name = "Cash"))
            val defaultWallet = dao.wallets().let { l -> (l.firstOrNull { !it.archived } ?: l.first()).id }

            suspend fun wallet(name: String): Long =
                if (name.isBlank()) defaultWallet else wallets.getOrPut(normalize(name)) { db.walletDao().upsert(WalletEntity(name = name.trim())) }

            suspend fun category(type: TxType, parent: String, sub: String): Long {
                val pKey = type to normalize(parent)
                val pid = parents[pKey] ?: catDao.insert(
                    CategoryEntity(name = parent.trim(), type = type, iconKey = "More", toneKey = "Gray", sortOrder = catDao.maxSort(type, null) + 1),
                ).also { parents[pKey] = it; created++ }
                if (sub.isBlank()) return pid
                val cKey = pid to normalize(sub)
                return children[cKey] ?: catDao.insert(
                    CategoryEntity(name = sub.trim(), type = type, parentId = pid, iconKey = "More", toneKey = "Gray", sortOrder = catDao.maxSort(type, pid) + 1),
                ).also { children[cKey] = it; created++ }
            }

            val pending = ArrayList<Pending>()
            val errors = ArrayList<String>()
            var errorCount = 0
            fun fail(line: Int, why: String) { errorCount++; if (errors.size < 5) errors.add("Line $line: $why") }

            rows.forEachIndexed { idx, row ->
                if (idx == 0 && hasHeader) return@forEachIndexed
                val line = idx + 1
                val ts = TransactionTable.parseDate(cell(row, cols.date)) ?: return@forEachIndexed fail(line, "invalid date \"${cell(row, cols.date)}\"")
                val type = TransactionTable.parseType(cell(row, cols.type)) ?: return@forEachIndexed fail(line, "invalid type \"${cell(row, cols.type)}\"")
                val amount = TransactionTable.parseAmount(cell(row, cols.amount)) ?: return@forEachIndexed fail(line, "invalid amount \"${cell(row, cols.amount)}\"")
                val note = CsvCodec.restoreFormula(cell(row, cols.note))
                val catName = CsvCodec.restoreFormula(cell(row, cols.category))
                val subName = CsvCodec.restoreFormula(cell(row, cols.sub))
                when (type) {
                    TxType.EXPENSE, TxType.INCOME -> {
                        // A blank category is valid: debt payments are stored without one.
                        val catId = if (catName.isBlank()) null else category(type, catName, subName)
                        pending.add(Pending(ts, type, amount, wallet(cell(row, cols.wallet)), catId, null, null, note))
                    }
                    TxType.SAVING -> {
                        val vName = cell(row, cols.vault)
                        if (vName.isBlank()) return@forEachIndexed fail(line, "missing savings vault name")
                        val vid = vaults.getOrPut(normalize(vName)) { db.vaultDao().upsert(VaultEntity(name = vName.trim(), target = 0)) }
                        pending.add(Pending(ts, type, amount, wallet(cell(row, cols.wallet)), null, vid, null, note))
                    }
                    TxType.TRANSFER -> {
                        val from = wallet(cell(row, cols.wallet))
                        val toName = cell(row, cols.toWallet)
                        if (toName.isBlank()) return@forEachIndexed fail(line, "missing destination wallet")
                        val to = wallet(toName)
                        if (to == from) return@forEachIndexed fail(line, "source and destination wallets are the same")
                        pending.add(Pending(ts, type, amount, from, null, null, to, note))
                    }
                }
            }

            // Duplicate check against what was in the DB BEFORE this import (identical rows inside the file are all kept).
            var imported = 0
            var duplicates = 0
            for ((_, group) in pending.groupBy { it }) {
                val p = group.first()
                val minute = p.ts - Math.floorMod(p.ts, 60_000L)
                val existing = dao.countSame(minute, minute + 60_000, p.amount, p.type, p.walletId, p.note, p.categoryId, p.vaultId, p.toWalletId)
                val toInsert = (group.size - existing).coerceAtLeast(0)
                duplicates += group.size - toInsert
                dao.insertTransactions(
                    group.take(toInsert).map { TransactionEntity(walletId = it.walletId, type = it.type, amount = it.amount, categoryId = it.categoryId, vaultId = it.vaultId, toWalletId = it.toWalletId, note = it.note, timestamp = it.ts) },
                )
                imported += toInsert
            }
            ImportResult(imported, duplicates, errorCount, errors, created)
        }
    }

    // ---------------- restore ----------------

    fun readSnapshot(input: InputStream): Snapshot = JsonBackup.read(input)

    /** REPLACES all data with the snapshot, atomically: any failure rolls back and leaves current data untouched. */
    suspend fun restore(snapshot: Snapshot): RestoreResult = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.clearDebtPayments(); dao.clearDebts(); dao.clearBudgets(); dao.clearTransactions()
            dao.clearVaults(); dao.clearCategories(); dao.clearWallets()
            dao.insertWallets(snapshot.wallets)
            // parents before children (self-referencing foreign key)
            dao.insertCategories(snapshot.categories.sortedBy { it.parentId != null })
            dao.insertVaults(snapshot.vaults)
            dao.insertTransactions(snapshot.transactions)
            dao.insertBudgets(snapshot.budgets)
            dao.insertDebts(snapshot.debts)
            dao.insertDebtPayments(snapshot.debtPayments)
            RestoreResult(snapshot.transactions.size, snapshot.categories.size, snapshot.wallets.size, snapshot.debts.size)
        }
    }

    /** Wipes every table and restores the starter wallet/categories/fund, atomically (failure leaves data untouched). */
    suspend fun resetAll() = withContext(Dispatchers.IO) {
        db.withTransaction {
            dao.clearDebtPayments(); dao.clearDebts(); dao.clearBudgets(); dao.clearTransactions()
            dao.clearVaults(); dao.clearCategories(); dao.clearWallets()
            dao.insertWallets(DefaultData.wallets)
            dao.insertCategories(DefaultData.categories)
            dao.insertVaults(DefaultData.vaults)
        }
    }

    suspend fun counts() = dao.counts()
}
