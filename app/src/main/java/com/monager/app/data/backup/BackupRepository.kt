package com.monager.app.data.backup

import androidx.room.withTransaction
import com.monager.app.data.local.DefaultData
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.local.entity.BudgetEntity
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

/** [budgets] / [savings]: rows of the Budgets / Savings tables that were created or updated. */
data class ImportResult(
    val imported: Int, val duplicates: Int, val errors: Int, val firstErrors: List<String>, val createdCategories: Int,
    val budgets: Int = 0, val savings: Int = 0,
)
data class RestoreResult(val transactions: Int, val categories: Int, val wallets: Int, val debts: Int, val budgets: Int, val savings: Int)

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

    /** Three sheets: Transactions, Budgets, Savings (all read in one DB transaction so they agree with each other). */
    suspend fun exportXlsx(out: OutputStream) = withContext(Dispatchers.IO) {
        val sheets = db.withTransaction {
            listOf(
                XlsxCodec.Sheet("Transactions", TransactionTable.toRows(dao.exportRows(), neutralize = false), setOf(TransactionTable.AMOUNT_COLUMN)),
                XlsxCodec.Sheet("Budgets", PlanTables.BudgetTable.toRows(dao.budgetExportRows(), neutralize = false), setOf(PlanTables.BudgetTable.LIMIT_COLUMN)),
                XlsxCodec.Sheet("Savings", PlanTables.SavingsTable.toRows(dao.vaultExportRows(), neutralize = false), PlanTables.SavingsTable.NUMERIC_COLUMNS),
            )
        }
        XlsxCodec.write(sheets, out)
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

    /** Every table in the file: each worksheet of an Excel file, or the single table of a CSV file. */
    suspend fun readTables(kind: FileKind, input: InputStream): List<List<List<String>>> = withContext(Dispatchers.IO) {
        when (kind) {
            FileKind.XLSX -> XlsxCodec.readAll(input)
            FileKind.CSV -> listOf(CsvCodec.parse(input.readBytes().toString(Charsets.UTF_8)))
            FileKind.JSON -> throw IllegalArgumentException("JSON is not a table")
        }
    }

    suspend fun readTable(kind: FileKind, input: InputStream): List<List<String>> = readTables(kind, input).first()

    private data class Pending(val ts: Long, val type: TxType, val amount: Long, val walletId: Long, val categoryId: Long?, val vaultId: Long?, val toWalletId: Long?, val note: String)

    /** Lookups and counters shared by all tables of one import (one DB transaction). */
    private inner class ImportRun {
        private val catDao = db.categoryDao()
        private val wallets = HashMap<String, Long>()
        val vaults = HashMap<String, VaultEntity>()
        private val parents = HashMap<Pair<TxType, String>, Long>()
        private val children = HashMap<Pair<Long, String>, Long>()
        private var defaultWallet: Long? = null
        var createdCategories = 0
        val errors = ArrayList<String>()
        var errorCount = 0

        suspend fun load() {
            dao.wallets().forEach { wallets.putIfAbsent(normalize(it.name), it.id) }
            dao.vaults().forEach { vaults.putIfAbsent(normalize(it.name), it) }
            dao.categories().forEach { c ->
                if (c.parentId == null) parents.putIfAbsent(c.type to normalize(c.name), c.id) else children.putIfAbsent(c.parentId to normalize(c.name), c.id)
            }
        }

        /** [sheet] null for transaction rows (messages read "Line 3: ..."), otherwise e.g. "Budgets, line 3: ...". */
        fun fail(sheet: String?, line: Int, why: String) {
            errorCount++
            if (errors.size < 5) errors.add(if (sheet == null) "Line $line: $why" else "$sheet, line $line: $why")
        }

        /** Only needed by transaction rows, so a budgets-only file does not create a "Cash" wallet. */
        private suspend fun defaultWallet(): Long = defaultWallet ?: run {
            if (wallets.isEmpty()) wallets[normalize("Cash")] = db.walletDao().upsert(WalletEntity(name = "Cash"))
            dao.wallets().let { l -> (l.firstOrNull { !it.archived } ?: l.first()).id }
        }.also { defaultWallet = it }

        suspend fun wallet(name: String): Long =
            if (name.isBlank()) defaultWallet() else wallets.getOrPut(normalize(name)) { db.walletDao().upsert(WalletEntity(name = name.trim())) }

        suspend fun vault(name: String): Long =
            vaults.getOrPut(normalize(name)) { VaultEntity(name = name.trim(), target = 0).let { it.copy(id = db.vaultDao().upsert(it)) } }.id

        suspend fun category(type: TxType, parent: String, sub: String): Long {
            val pKey = type to normalize(parent)
            val pid = parents[pKey] ?: catDao.insert(
                CategoryEntity(name = parent.trim(), type = type, iconKey = "More", toneKey = "Gray", sortOrder = catDao.maxSort(type, null) + 1),
            ).also { parents[pKey] = it; createdCategories++ }
            if (sub.isBlank()) return pid
            val cKey = pid to normalize(sub)
            return children[cKey] ?: catDao.insert(
                CategoryEntity(name = sub.trim(), type = type, parentId = pid, iconKey = "More", toneKey = "Gray", sortOrder = catDao.maxSort(type, pid) + 1),
            ).also { children[cKey] = it; createdCategories++ }
        }
    }

    suspend fun importTable(rows: List<List<String>>): ImportResult = importTables(listOf(rows))

    /**
     * Imports every table of a CSV/Excel file in one DB transaction (any failure leaves the data untouched).
     * Each table is recognised by its header: savings goals first (so Saving rows find their goal and its target),
     * then transactions, then budgets.
     *
     * - Transactions are appended. Rows that already exist (same minute, amount, type, wallet, category, note) are
     *   skipped, so importing the same file twice does not double your data.
     * - Savings goals and budgets are matched by name / (month, category): existing ones are updated, others created.
     * - Unknown categories, wallets and savings goals are created.
     */
    suspend fun importTables(tables: List<List<List<String>>>): ImportResult = withContext(Dispatchers.IO) {
        db.withTransaction {
            val run = ImportRun().apply { load() }
            val byKind = tables.filter { it.isNotEmpty() }.groupBy { PlanTables.classify(it.first()) }
            var savings = 0
            byKind[TableKind.SAVINGS].orEmpty().forEach { savings += importSavings(it, run) }
            var imported = 0
            var duplicates = 0
            byKind[TableKind.TRANSACTIONS].orEmpty().forEach { val (i, d) = importTransactions(it, run); imported += i; duplicates += d }
            var budgets = 0
            byKind[TableKind.BUDGETS].orEmpty().forEach { budgets += importBudgets(it, run) }
            ImportResult(imported, duplicates, run.errorCount, run.errors, run.createdCategories, budgets, savings)
        }
    }

    /** Creates or updates savings goals by name; the Saved column is ignored (it is recomputed from Saving transactions). */
    private suspend fun importSavings(rows: List<List<String>>, run: ImportRun): Int {
        val cols = PlanTables.SavingsTable.columns(rows.first())
        val vaultDao = db.vaultDao()
        var count = 0
        rows.forEachIndexed { idx, row ->
            if (idx == 0) return@forEachIndexed
            val line = idx + 1
            val name = CsvCodec.restoreFormula(cell(row, cols.name))
            if (name.isBlank()) return@forEachIndexed run.fail("Savings", line, "missing name")
            val target = TransactionTable.parseAmount(cell(row, cols.target)) ?: return@forEachIndexed run.fail("Savings", line, "invalid target \"${cell(row, cols.target)}\"")
            val deadlineText = cell(row, cols.deadline)
            val deadline = if (deadlineText.isBlank()) null else PlanTables.SavingsTable.parseDeadline(deadlineText)
                ?: return@forEachIndexed run.fail("Savings", line, "invalid deadline \"$deadlineText\"")
            val icon = cell(row, cols.icon)
            val archived = PlanTables.SavingsTable.parseArchived(cell(row, cols.archived))
            val key = normalize(name)
            val existing = run.vaults[key]
            val v = existing?.copy(target = target, deadline = deadline, iconKey = icon.ifBlank { existing.iconKey }, archived = archived)
                ?: VaultEntity(name = name.trim(), target = target, iconKey = icon.ifBlank { "Sparkles" }, deadline = deadline, archived = archived)
            val id = vaultDao.upsert(v)
            run.vaults[key] = if (existing == null) v.copy(id = id) else v
            count++
        }
        return count
    }

    /** Sets the limit of each (month, category) budget, creating it when missing. Returns how many rows were applied. */
    private suspend fun importBudgets(rows: List<List<String>>, run: ImportRun): Int {
        val cols = PlanTables.BudgetTable.columns(rows.first())
        val budgetDao = db.budgetDao()
        var count = 0
        rows.forEachIndexed { idx, row ->
            if (idx == 0) return@forEachIndexed
            val line = idx + 1
            val month = PlanTables.BudgetTable.parseMonth(cell(row, cols.month)) ?: return@forEachIndexed run.fail("Budgets", line, "invalid month \"${cell(row, cols.month)}\"")
            val limit = TransactionTable.parseAmount(cell(row, cols.limit)) ?: return@forEachIndexed run.fail("Budgets", line, "invalid limit \"${cell(row, cols.limit)}\"")
            val catName = CsvCodec.restoreFormula(cell(row, cols.category))
            val categoryId = if (PlanTables.BudgetTable.isOverall(catName)) null else run.category(TxType.EXPENSE, catName, CsvCodec.restoreFormula(cell(row, cols.sub)))
            val existing = budgetDao.find(month, categoryId)
            budgetDao.upsert(existing?.copy(limitAmount = limit) ?: BudgetEntity(categoryId = categoryId, month = month, limitAmount = limit))
            count++
        }
        return count
    }

    /** Returns (imported, skipped duplicates). */
    private suspend fun importTransactions(rows: List<List<String>>, run: ImportRun): Pair<Int, Int> {
        val (cols, hasHeader) = TransactionTable.detectColumns(rows.first())
        val pending = ArrayList<Pending>()
        fun fail(line: Int, why: String) = run.fail(null, line, why)
        suspend fun wallet(name: String) = run.wallet(name)
        suspend fun category(type: TxType, parent: String, sub: String) = run.category(type, parent, sub)

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
                    pending.add(Pending(ts, type, amount, wallet(cell(row, cols.wallet)), null, run.vault(vName), null, note))
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
        return imported to duplicates
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
            RestoreResult(snapshot.transactions.size, snapshot.categories.size, snapshot.wallets.size, snapshot.debts.size, snapshot.budgets.size, snapshot.vaults.size)
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
