package com.monager.app.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.local.SeedCallback
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.WalletEntity
import com.monager.app.data.repository.DebtRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BackupTest {
    private lateinit var db: MonagerDatabase
    private lateinit var repo: BackupRepository
    private val opened = ArrayList<MonagerDatabase>()

    private fun newDb(seed: Boolean): MonagerDatabase {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val b = Room.inMemoryDatabaseBuilder(ctx, MonagerDatabase::class.java).allowMainThreadQueries()
        if (seed) b.addCallback(SeedCallback)
        return b.build().also { it.openHelper.writableDatabase; opened.add(it) }
    }

    @Before fun setUp() { db = newDb(seed = true); repo = BackupRepository(db) }
    @After fun tearDown() = opened.forEach { it.close() }

    private fun day(d: Int, hour: Int = 9) = LocalDate.of(2026, 11, d).atTime(hour, 15).atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun populate() = runBlocking {
        val dao = db.transactionDao()
        dao.insert(TransactionEntity(walletId = 1, type = TxType.EXPENSE, amount = 45_000, categoryId = 104, note = "cà phê, \"đặc biệt\"", timestamp = day(3)))
        dao.insert(TransactionEntity(walletId = 1, type = TxType.EXPENSE, amount = 120_000, categoryId = 2, note = "=HYPERLINK(\"x\")", timestamp = day(3, 10)))
        dao.insert(TransactionEntity(walletId = 1, type = TxType.INCOME, amount = 18_000_000, categoryId = 21, timestamp = day(4)))
        dao.insert(TransactionEntity(walletId = 1, type = TxType.SAVING, amount = 500_000, vaultId = 1, timestamp = day(5)))
        val w2 = db.walletDao().upsert(WalletEntity(name = "Ngân hàng"))
        dao.insert(TransactionEntity(walletId = 1, type = TxType.TRANSFER, amount = 1_000_000, toWalletId = w2, timestamp = day(6)))
        db.budgetDao().upsert(BudgetEntity(categoryId = null, month = 202611, limitAmount = 10_000_000))
        val debts = DebtRepository(db)
        val id = debts.add(DebtEntity(personName = "Hoàng Nam", amount = 6_000_000, iLent = true, dueDate = day(20), createdAt = day(1)))
        debts.pay(id, 1_500_000, day(7), walletId = 1)
    }

    private fun snapshotOf(d: MonagerDatabase) = runBlocking {
        val b = d.backupDao()
        Snapshot(b.wallets(), b.categories(), b.vaults(), b.transactions(), b.budgets(), b.debts(), b.debtPayments())
    }

    // ---------- JSON backup ----------
    @Test fun jsonBackupRestoresEverythingExactlyIntoEmptyDatabase() {
        populate()
        val out = ByteArrayOutputStream(); runBlocking { repo.exportJson(out) }
        val fresh = newDb(seed = false)
        val result = runBlocking { BackupRepository(fresh).restore(BackupRepository(fresh).readSnapshot(ByteArrayInputStream(out.toByteArray()))) }
        assertEquals(snapshotOf(db), snapshotOf(fresh))
        assertEquals(snapshotOf(db).transactions.size, result.transactions)
    }

    @Test fun restoreReplacesExistingData() {
        populate()
        val out = ByteArrayOutputStream(); runBlocking { repo.exportJson(out) }
        runBlocking { db.transactionDao().insert(TransactionEntity(walletId = 1, type = TxType.EXPENSE, amount = 1, categoryId = 1, timestamp = day(9))) }
        runBlocking { repo.restore(repo.readSnapshot(ByteArrayInputStream(out.toByteArray()))) }
        assertEquals(6, snapshotOf(db).transactions.size) // 5 manual + 1 debt-collection transaction; the extra row is gone
    }

    @Test fun balanceSurvivesBackupAndRestore() {
        populate()
        val before = runBlocking { db.transactionDao().observeTotalBalance().first() }
        val out = ByteArrayOutputStream(); runBlocking { repo.exportJson(out) }
        val fresh = newDb(seed = false)
        runBlocking { BackupRepository(fresh).restore(BackupRepository(fresh).readSnapshot(ByteArrayInputStream(out.toByteArray()))) }
        assertEquals(before, runBlocking { fresh.transactionDao().observeTotalBalance().first() })
    }

    @Test fun invalidJsonIsRejectedWithoutTouchingData() {
        populate()
        val before = snapshotOf(db)
        for (bad in listOf("{", "not json", "{\"app\":\"other\",\"version\":1}", "{\"app\":\"monager\",\"version\":99}", "{\"app\":\"monager\",\"version\":1}")) {
            val threw = try { repo.readSnapshot(ByteArrayInputStream(bad.toByteArray())); false } catch (_: BackupFormatException) { true }
            assertTrue("should reject: $bad", threw)
        }
        assertEquals(before, snapshotOf(db))
    }

    @Test fun restoreWithBrokenReferencesRollsBackCompletely() {
        populate()
        val before = snapshotOf(db)
        val broken = before.copy(transactions = before.transactions + TransactionEntity(id = 9999, walletId = 777, type = TxType.EXPENSE, amount = 1, categoryId = 1, timestamp = 1))
        val threw = try { runBlocking { repo.restore(broken) }; false } catch (_: Exception) { true }
        assertTrue(threw)
        assertEquals(before, snapshotOf(db)) // old data intact
    }

    // ---------- CSV / Excel round trip ----------
    private fun exportRowsAsCsv(): List<List<String>> {
        val out = ByteArrayOutputStream(); runBlocking { repo.exportCsv(out) }
        return CsvCodec.parse(out.toByteArray().toString(Charsets.UTF_8))
    }

    @Test fun csvExportThenImportIntoEmptyDatabaseRecreatesTransactions() {
        populate()
        val rows = exportRowsAsCsv()
        assertEquals(TransactionTable.HEADER, rows.first())
        val fresh = newDb(seed = false)
        val result = runBlocking { BackupRepository(fresh).importTable(rows) }
        assertEquals(6, result.imported); assertEquals(0, result.errors)
        val imported = snapshotOf(fresh).transactions
        assertEquals(snapshotOf(db).transactions.map { it.amount }.sorted(), imported.map { it.amount }.sorted())
        assertEquals(setOf(TxType.EXPENSE, TxType.INCOME, TxType.SAVING, TxType.TRANSFER), imported.map { it.type }.toSet())
        assertEquals(2, snapshotOf(fresh).wallets.size)
        assertTrue(snapshotOf(fresh).categories.any { it.name == "Beverages" && it.parentId != null })
    }

    @Test fun csvFormulaNoteIsNeutralizedInFileButRestoredOnImport() {
        populate()
        val rows = exportRowsAsCsv()
        assertTrue(rows.any { it.last().startsWith("'=HYPERLINK") })
        val fresh = newDb(seed = false)
        runBlocking { BackupRepository(fresh).importTable(rows) }
        assertTrue(snapshotOf(fresh).transactions.any { it.note == "=HYPERLINK(\"x\")" })
    }

    @Test fun importingTheSameFileTwiceSkipsDuplicates() {
        populate()
        val rows = exportRowsAsCsv()
        val fresh = newDb(seed = false)
        val r = BackupRepository(fresh)
        assertEquals(6, runBlocking { r.importTable(rows) }.imported)
        val second = runBlocking { r.importTable(rows) }
        assertEquals(0, second.imported); assertEquals(6, second.duplicates)
        assertEquals(6, snapshotOf(fresh).transactions.size)
    }

    @Test fun reImportingIntoTheSourceDatabaseAddsNothing() {
        populate()
        val before = snapshotOf(db).transactions.size
        val second = runBlocking { repo.importTable(exportRowsAsCsv()) }
        assertEquals(0, second.imported)
        assertEquals(before, snapshotOf(db).transactions.size)
    }

    @Test fun identicalRowsInsideOneFileAreAllKept() {
        val rows = listOf(TransactionTable.HEADER) + List(3) { listOf("2026-11-03 09:15", "Expense", "30000", "Food", "Beverages", "Cash", "", "", "") }
        assertEquals(3, runBlocking { repo.importTable(rows) }.imported)
    }

    @Test fun badRowsAreReportedAndSkippedGoodRowsStillImport() {
        val rows = listOf(
            TransactionTable.HEADER,
            listOf("2026-11-03 09:15", "Expense", "30000", "Food", "", "Cash", "", "", ""),
            listOf("yesterday", "Expense", "30000", "Food", "", "", "", "", ""),
            listOf("2026-11-03 09:15", "Linh tinh", "30000", "Food", "", "", "", "", ""),
            listOf("2026-11-03 09:15", "Expense", "abc", "Food", "", "", "", "", ""),
            listOf("2026-11-03 09:15", "Expense", "5000", "", "", "", "", "", ""),
            listOf("2026-11-03 09:15", "Transfer", "5000", "", "", "Cash", "Cash", "", ""),
        )
        val r = runBlocking { repo.importTable(rows) }
        assertEquals(2, r.imported); assertEquals(4, r.errors) // the blank-category row is valid
        assertTrue(r.firstErrors.first().startsWith("Line 3:"))
    }

    @Test fun importWorksWithoutHeaderAndWithReorderedHeader() {
        val noHeader = listOf(listOf("03/11/2026 09:15", "Income", "1.500.000", "Salary", "", "", "", "", ""))
        assertEquals(1, runBlocking { repo.importTable(noHeader) }.imported)
        val reordered = listOf(listOf("Amount", "Type", "Date", "Category"), listOf("20000", "chi tieu", "04/11/2026", "Food"))
        assertEquals(1, runBlocking { repo.importTable(reordered) }.imported)
    }

    @Test fun excelExportThenImportRoundTrip() {
        populate()
        val out = ByteArrayOutputStream(); runBlocking { repo.exportXlsx(out) }
        val fresh = newDb(seed = false)
        val r = BackupRepository(fresh)
        val kind = r.detect(BufferedInputStream(ByteArrayInputStream(out.toByteArray())))
        assertEquals(FileKind.XLSX, kind)
        val table = runBlocking { r.readTable(kind, ByteArrayInputStream(out.toByteArray())) }
        assertEquals(6, runBlocking { r.importTable(table) }.imported)
        assertTrue(snapshotOf(fresh).transactions.any { it.note == "=HYPERLINK(\"x\")" }) // no apostrophe artefacts in xlsx
    }

    @Test fun fileKindDetection() {
        fun kind(b: ByteArray) = repo.detect(BufferedInputStream(ByteArrayInputStream(b)))
        assertEquals(FileKind.JSON, kind("  {\"app\":1}".toByteArray())); assertEquals(FileKind.JSON, kind("﻿{".toByteArray()))
        assertEquals(FileKind.XLSX, kind(byteArrayOf('P'.code.toByte(), 'K'.code.toByte(), 3, 4)))
        assertEquals(FileKind.CSV, kind("Date,Type".toByteArray())); assertFalse(kind("a".toByteArray()) == FileKind.JSON)
    }
}
