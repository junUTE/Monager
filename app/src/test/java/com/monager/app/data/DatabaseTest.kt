package com.monager.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.monager.app.data.local.MonagerDatabase
import com.monager.app.data.local.SeedCallback
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.WalletEntity
import com.monager.app.data.repository.BudgetRepository
import com.monager.app.data.repository.CategoryRepository
import com.monager.app.data.repository.DebtRepository
import com.monager.app.data.repository.TransactionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DatabaseTest {
    private lateinit var db: MonagerDatabase
    private lateinit var tx: TransactionRepository
    private lateinit var cats: CategoryRepository
    private lateinit var budgets: BudgetRepository
    private lateinit var debts: DebtRepository

    private val from = 1_000L
    private val to = 1_000_000L

    @Before fun setUp() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(ctx, MonagerDatabase::class.java).allowMainThreadQueries().addCallback(SeedCallback).build()
        tx = TransactionRepository(db); cats = CategoryRepository(db); budgets = BudgetRepository(db); debts = DebtRepository(db)
        db.openHelper.writableDatabase // force onCreate -> seed
    }

    @After fun tearDown() = db.close()

    private fun expense(amount: Long, cat: Long, ts: Long = 5_000) =
        TransactionEntity(walletId = 1, type = TxType.EXPENSE, amount = amount, categoryId = cat, timestamp = ts)

    @Test fun seedCreatesWalletAndTwoLevelCategories() = runBlocking {
        val tree = cats.tree(TxType.EXPENSE).first()
        assertEquals(6, tree.size)
        assertEquals(listOf("Ăn sáng", "Ăn trưa & tối", "Cà phê"), tree.first { it.parent.id == 1L }.children.map { it.name })
        assertEquals(1, db.walletDao().observeActive().first().size)
    }

    @Test fun balanceIsIncomeMinusExpenseMinusSavings() = runBlocking {
        tx.add(TransactionEntity(walletId = 1, type = TxType.INCOME, amount = 1_000, categoryId = 11, timestamp = 2_000))
        tx.add(expense(300, 1))
        tx.add(TransactionEntity(walletId = 1, type = TxType.SAVING, amount = 200, vaultId = 1, timestamp = 3_000))
        assertEquals(500L, db.transactionDao().observeTotalBalance().first())
        assertEquals(200L, db.vaultDao().observeWithSaved().first().first().saved)
    }

    @Test fun periodTotalsRespectRange() = runBlocking {
        tx.add(expense(100, 1, 5_000)); tx.add(expense(50, 1, 2_000_000))
        assertEquals(100L, tx.total(TxType.EXPENSE, from, to).first())
    }

    @Test fun totalsRollUpChildrenToParent() = runBlocking {
        tx.add(expense(100, 101)); tx.add(expense(200, 103)); tx.add(expense(50, 2))
        val groups = tx.totalsByParent(TxType.EXPENSE, from, to).first()
        assertEquals(1L, groups[0].groupId); assertEquals(300L, groups[0].total)
        assertEquals(2L, groups[1].groupId); assertEquals(50L, groups[1].total)
    }

    @Test fun recentRowsAreJoinedAndOrdered() = runBlocking {
        tx.add(expense(100, 101, 3_000)); tx.add(expense(10, 2, 4_000))
        val rows = tx.recent(10).first()
        assertEquals(listOf("Di chuyển", "Ăn sáng"), rows.map { it.categoryName })
        assertEquals("Ăn uống", rows[1].parentName)
    }

    @Test fun addValidatesInvariants() {
        runBlocking {
            assertThrows { tx.add(expense(0, 1)) }
            assertThrows { tx.add(TransactionEntity(walletId = 1, type = TxType.EXPENSE, amount = 5, timestamp = 1)) }
            assertThrows { tx.add(TransactionEntity(walletId = 1, type = TxType.SAVING, amount = 5, timestamp = 1)) }
            assertThrows { tx.add(TransactionEntity(walletId = 1, type = TxType.TRANSFER, amount = 5, toWalletId = 1, timestamp = 1)) }
        }
    }

    private suspend fun assertThrows(block: suspend () -> Unit) {
        val threw = try { block(); false } catch (_: IllegalArgumentException) { true }
        assertTrue("expected IllegalArgumentException", threw)
    }

    @Test fun transfersDoNotChangeTotalBalance() = runBlocking {
        val w2 = db.walletDao().upsert(WalletEntity(name = "Ngân hàng"))
        tx.add(TransactionEntity(walletId = 1, type = TxType.INCOME, amount = 1_000, categoryId = 11, timestamp = 2_000))
        tx.add(TransactionEntity(walletId = 1, type = TxType.TRANSFER, amount = 400, toWalletId = w2, timestamp = 3_000))
        assertEquals(1_000L, db.transactionDao().observeTotalBalance().first())
    }

    @Test fun budgetSpentIncludesChildrenAndOverallBudgetCountsEverything() = runBlocking {
        tx.add(expense(100, 101)); tx.add(expense(200, 1)); tx.add(expense(50, 2))
        budgets.set(202610, 1, 1_000); budgets.set(202610, null, 5_000)
        val list = budgets.forMonth(202610, from, to).first()
        assertEquals(350L, list.first { it.categoryId == null }.spent)
        assertEquals(300L, list.first { it.categoryId == 1L }.spent)
    }

    @Test fun budgetIsUniquePerCategoryAndMonth() = runBlocking {
        budgets.set(202610, null, 1_000); budgets.set(202610, null, 2_000); budgets.set(202610, 1, 500)
        val list = budgets.forMonth(202610, from, to).first()
        assertEquals(2, list.size)
        assertEquals(2_000L, list.first { it.categoryId == null }.limitAmount)
    }

    @Test fun categoryAllowsOnlyTwoLevels() = runBlocking {
        val sub = cats.add("Trà sữa", TxType.EXPENSE, 1, "Coffee", "Mint")
        assertThrows { cats.add("Quá sâu", TxType.EXPENSE, sub, "Coffee", "Mint") }
    }

    @Test fun usedCategoryCannotBeDeletedButUnusedCan() = runBlocking {
        tx.add(expense(100, 101))
        assertFalse(cats.deleteIfUnused(1))   // child 101 is used
        assertTrue(cats.deleteIfUnused(6))    // "Khác" unused
        assertNull(db.categoryDao().get(6))
    }

    @Test fun reorderPersistsSortOrder() = runBlocking {
        val parents = cats.tree(TxType.EXPENSE).first().map { it.parent }
        cats.reorder(parents.reversed())
        assertEquals(parents.reversed().map { it.id }, cats.tree(TxType.EXPENSE).first().map { it.parent.id })
    }

    @Test fun partialDebtPaymentCreatesLinkedTransaction() = runBlocking {
        val id = debts.add(DebtEntity(personName = "Nam", amount = 1_000, iLent = true, createdAt = 1))
        assertTrue(debts.pay(id, 400, 2_000, walletId = 1))
        val d = debts.debts(true).first().single()
        assertEquals(400L, d.paid)
        assertEquals(400L, db.transactionDao().observeTotal(TxType.INCOME, from, to).first()) // collected = income
    }

    @Test fun debtPaymentCannotExceedRemaining() = runBlocking {
        val id = debts.add(DebtEntity(personName = "Linh", amount = 1_000, iLent = false, createdAt = 1))
        assertTrue(debts.pay(id, 700, 2_000, walletId = null))
        assertFalse(debts.pay(id, 400, 3_000, walletId = null))
        assertEquals(700L, debts.debts(false).first().single().paid)
    }

    @Test fun insertedCategoryHasNoDuplicateConstraintIssues() = runBlocking {
        val id = db.categoryDao().insert(CategoryEntity(name = "X", type = TxType.EXPENSE, iconKey = "More", toneKey = "Gray"))
        assertTrue(id > 0)
    }
}
