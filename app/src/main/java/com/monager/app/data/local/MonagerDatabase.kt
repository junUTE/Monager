package com.monager.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.monager.app.data.local.dao.BackupDao
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
import com.monager.app.data.local.entity.TxType
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
    abstract fun backupDao(): BackupDao

    companion object {
        /** [seed] true in production; tests pass false to start from an empty database. */
        fun build(context: Context, seed: Boolean = true): MonagerDatabase =
            Room.databaseBuilder(context, MonagerDatabase::class.java, "monager.db")
                .apply { if (seed) addCallback(SeedCallback) }
                // Never fallbackToDestructiveMigration: user finance data must survive updates.
                .build()
    }
}

/** Starter data: used when the database is first created and when the user resets everything. */
object DefaultData {
    val wallets = listOf(WalletEntity(id = 1, name = "Cash", initialBalance = 0, iconKey = "Wallet", sortOrder = 0))
    val vaults = listOf(VaultEntity(id = 1, name = "Emergency fund", target = 50_000_000, iconKey = "Sparkles"))

    /** A parent category plus its subcategories as "Name|IconKey". Names must not contain a single quote (seeded via raw SQL). */
    private class Cat(val id: Long, val name: String, val type: TxType, val icon: String, val tone: String, vararg val subs: String)

    private val tree = listOf(
        Cat(1, "Food", TxType.EXPENSE, "Utensils", "Mint", "Eating out|Noodles", "Snacks|Popcorn", "Daily food|Basket", "Beverages|Coffee"),
        Cat(2, "Vehicle", TxType.EXPENSE, "Car", "Blue", "Fuel|Fuel", "Maintenance|Settings", "Repairs|Wrench", "Parking|Parking", "Bus & train|Bus"),
        Cat(3, "Beauty", TxType.EXPENSE, "Lipstick", "Purple", "Haircut|Scissors", "Cosmetics|Lipstick", "Makeup|Sparkles", "Accessories|Gem"),
        Cat(4, "Household", TxType.EXPENSE, "House", "Peach", "Appliances|Tv", "Furniture|Bed", "Kitchen|CookingPot", "Toiletries|SprayBottle", "Supplies|Cart"),
        Cat(5, "Sport", TxType.EXPENSE, "Ball", "Sage", "Football|Ball", "Swimming|Waves", "Billiards|Billiard"),
        Cat(6, "Service fees", TxType.EXPENSE, "Receipt", "Yellow", "Rent|Home", "Phone bill|Phone"),
        Cat(7, "Apparel", TxType.EXPENSE, "Shirt", "Peach", "Clothing|Shirt", "Fashion|Dress", "Shoes|Shoe", "Laundry|Soap"),
        Cat(8, "Health", TxType.EXPENSE, "Heart", "Purple", "Hospital|Hospital", "Medicine|Pill"),
        Cat(9, "Education", TxType.EXPENSE, "Graduation", "Blue", "Schooling|Graduation", "Textbooks|Book", "Academy|Users", "Materials|FileCheck"),
        Cat(10, "Technology", TxType.EXPENSE, "Laptop", "Blue", "Phone|Phone", "Laptop|Laptop", "Other tech|Gamepad"),
        Cat(11, "Investment", TxType.EXPENSE, "TrendingUp", "Sage"),
        Cat(12, "Gift", TxType.EXPENSE, "Gift", "Yellow"),
        Cat(13, "Other", TxType.EXPENSE, "More", "Gray"),
        Cat(21, "Salary", TxType.INCOME, "Briefcase", "Sage"),
        Cat(22, "Allowance", TxType.INCOME, "Banknote", "Mint"),
        Cat(23, "Petty cash", TxType.INCOME, "Coins", "Yellow"),
        Cat(24, "Bonus", TxType.INCOME, "Trophy", "Peach"),
        Cat(25, "Dividends", TxType.INCOME, "PiggyBank", "Sage"),
        Cat(26, "Stock sales", TxType.INCOME, "Candles", "Blue"),
        Cat(27, "Gift received", TxType.INCOME, "Gift", "Yellow"),
    )

    /** Parents first (self-referencing foreign key), then subcategories inheriting type/tone from their parent. */
    val categories: List<CategoryEntity> = run {
        val parents = tree.mapIndexed { i, c ->
            CategoryEntity(id = c.id, name = c.name, type = c.type, parentId = null, iconKey = c.icon, toneKey = c.tone, sortOrder = i)
        }
        parents + tree.flatMap { c ->
            c.subs.mapIndexed { i, s ->
                val (name, icon) = s.split("|")
                CategoryEntity(id = c.id * 100 + i + 1, name = name, type = c.type, parentId = c.id, iconKey = icon, toneKey = c.tone, sortOrder = i)
            }
        }
    }
}

/** Runs once, when the database file is first created. Plain SQL: no DAO/thread needed. */
object SeedCallback : RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        DefaultData.wallets.forEach {
            db.execSQL("INSERT INTO wallets (id, name, initialBalance, iconKey, archived, sortOrder) VALUES (${it.id}, '${it.name}', ${it.initialBalance}, '${it.iconKey}', 0, ${it.sortOrder})")
        }
        DefaultData.vaults.forEach {
            db.execSQL("INSERT INTO vaults (id, name, target, iconKey, archived) VALUES (${it.id}, '${it.name}', ${it.target}, '${it.iconKey}', 0)")
        }
        DefaultData.categories.forEach {
            db.execSQL("INSERT INTO categories (id, name, type, parentId, iconKey, toneKey, sortOrder) VALUES (${it.id}, '${it.name}', '${it.type.name}', ${it.parentId ?: "NULL"}, '${it.iconKey}', '${it.toneKey}', ${it.sortOrder})")
        }
    }
}
