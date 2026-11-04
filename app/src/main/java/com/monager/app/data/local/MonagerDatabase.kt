package com.monager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.monager.app.data.local.dao.BudgetDao
import com.monager.app.data.local.dao.CategoryDao
import com.monager.app.data.local.dao.DebtDao
import com.monager.app.data.local.dao.TransactionDao
import com.monager.app.data.local.dao.VaultDao
import com.monager.app.data.local.dao.WalletDao
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.Converters
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.DebtPaymentEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity

@Database(
    entities = [
        WalletEntity::class, CategoryEntity::class, VaultEntity::class, TransactionEntity::class,
        BudgetEntity::class, DebtEntity::class, DebtPaymentEntity::class,
    ],
    version = 1,
    exportSchema = true, // app/schemas is committed -> enables migration tests
)
@TypeConverters(Converters::class)
abstract class MonagerDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun walletDao(): WalletDao
    abstract fun budgetDao(): BudgetDao
    abstract fun vaultDao(): VaultDao
    abstract fun debtDao(): DebtDao

    companion object {
        /** [seed] true in production; tests pass false to start from an empty database. */
        fun build(context: Context, seed: Boolean = true): MonagerDatabase =
            Room.databaseBuilder(context, MonagerDatabase::class.java, "monager.db")
                .apply { if (seed) addCallback(SeedCallback) }
                // Never fallbackToDestructiveMigration: user finance data must survive updates.
                .build()
    }
}

/** Runs once, when the database file is first created. Plain SQL: no DAO/thread needed. */
object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        db.execSQL("INSERT INTO wallets (id, name, initialBalance, iconKey, archived, sortOrder) VALUES (1, 'Tiền mặt', 0, 'Wallet', 0, 0)")
        db.execSQL("INSERT INTO vaults (id, name, target, iconKey, archived) VALUES (1, 'Quỹ dự phòng', 50000000, 'Sparkles', 0)")
        // id, name, type, parentId, iconKey, toneKey, sortOrder
        val rows = listOf(
            Triple(1, "Ăn uống", "EXPENSE|Utensils|Mint"), Triple(2, "Di chuyển", "EXPENSE|Car|Blue"),
            Triple(3, "Mua sắm", "EXPENSE|ShoppingBag|Peach"), Triple(4, "Hóa đơn", "EXPENSE|Receipt|Yellow"),
            Triple(5, "Sức khỏe", "EXPENSE|Heart|Purple"), Triple(6, "Khác", "EXPENSE|More|Gray"),
            Triple(11, "Lương", "INCOME|Briefcase|Sage"), Triple(12, "Quà tặng", "INCOME|Gift|Yellow"),
        )
        rows.forEachIndexed { i, (id, name, meta) ->
            val (type, icon, tone) = meta.split("|")
            db.execSQL("INSERT INTO categories (id, name, type, parentId, iconKey, toneKey, sortOrder) VALUES ($id, '$name', '$type', NULL, '$icon', '$tone', $i)")
        }
        val subs = listOf(
            Triple(101, 1, "Ăn sáng|Coffee"), Triple(102, 1, "Ăn trưa & tối|Utensils"), Triple(103, 1, "Cà phê|Coffee"),
            Triple(104, 2, "Xăng xe|Car"), Triple(105, 2, "Taxi & Grab|Car"),
            Triple(111, 11, "Lương chính|Briefcase"), Triple(112, 11, "Thưởng|Gift"),
        )
        val tones = mapOf(1 to ("EXPENSE" to "Mint"), 2 to ("EXPENSE" to "Blue"), 11 to ("INCOME" to "Sage"))
        subs.forEachIndexed { i, (id, parent, meta) ->
            val (name, icon) = meta.split("|")
            val (type, tone) = tones.getValue(parent)
            db.execSQL("INSERT INTO categories (id, name, type, parentId, iconKey, toneKey, sortOrder) VALUES ($id, '$name', '$type', $parent, '$icon', '$tone', $i)")
        }
    }
}
