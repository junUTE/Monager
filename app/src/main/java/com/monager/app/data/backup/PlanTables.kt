package com.monager.app.data.backup

import com.monager.app.data.backup.TransactionTable.normalize
import com.monager.app.data.local.dao.BudgetExportRow
import com.monager.app.data.local.dao.VaultWithSaved
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Which kind of rows a sheet (or CSV file) holds, decided from its header row. */
enum class TableKind { TRANSACTIONS, BUDGETS, SAVINGS }

/** Budgets and savings goals as tables, shared by the Excel export and the CSV/Excel import. Pure functions, no Android or DB. */
object PlanTables {
    private fun t(s: String?, neutralize: Boolean) = if (s == null) "" else if (neutralize) CsvCodec.neutralizeFormula(s) else s

    private fun header(row: List<String>) = row.map { normalize(it) }.toSet()

    /** Budgets need Month + Limit columns, savings goals a Target column; anything else is a transaction table. */
    fun classify(first: List<String>): TableKind {
        val h = header(first)
        return when {
            h.any { it in BudgetTable.MONTH } && h.any { it in BudgetTable.LIMIT } -> TableKind.BUDGETS
            h.any { it in SavingsTable.TARGET } -> TableKind.SAVINGS
            else -> TableKind.TRANSACTIONS
        }
    }

    internal fun find(first: List<String>, names: Set<String>, default: Int): Int =
        first.indexOfFirst { normalize(it) in names }.let { if (it >= 0) it else default }

    object BudgetTable {
        val HEADER = listOf("Month", "Category", "Subcategory", "Limit")
        const val LIMIT_COLUMN = 3
        /** Category cell of the overall monthly budget (a blank cell means the same on import). */
        const val OVERALL = "All expenses"

        internal val MONTH = setOf("month", "thang")
        internal val LIMIT = setOf("limit", "han muc", "ngan sach")
        private val CATEGORY = setOf("category", "danh muc")
        private val SUB = setOf("subcategory", "danh muc con")

        fun toRows(items: List<BudgetExportRow>, neutralize: Boolean): List<List<String>> =
            listOf(HEADER) + items.map {
                listOf(formatMonth(it.month), if (it.topName == null) OVERALL else t(it.topName, neutralize), t(it.subName, neutralize), it.limitAmount.toString())
            }

        class Columns(val month: Int, val category: Int, val sub: Int, val limit: Int)

        fun columns(first: List<String>) = Columns(find(first, MONTH, 0), find(first, CATEGORY, 1), find(first, SUB, 2), find(first, LIMIT, 3))

        fun isOverall(category: String) = category.isBlank() || normalize(category) == normalize(OVERALL)

        fun formatMonth(yyyyMM: Int) = "%04d-%02d".format(yyyyMM / 100, yyyyMM % 100)

        /** yyyyMM from "2026-11", "2026/11", "11/2026", "202611", a full date or an Excel date serial; null if unreadable. */
        fun parseMonth(text: String): Int? {
            val s = text.trim()
            fun of(y: Int, m: Int) = if (y in 1900..2999 && m in 1..12) y * 100 + m else null
            Regex("^(\\d{4})[-/.](\\d{1,2})$").find(s)?.let { return of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            Regex("^(\\d{1,2})[-/.](\\d{4})$").find(s)?.let { return of(it.groupValues[2].toInt(), it.groupValues[1].toInt()) }
            Regex("^(\\d{4})(\\d{2})$").find(s)?.let { return of(it.groupValues[1].toInt(), it.groupValues[2].toInt()) }
            val ms = TransactionTable.parseDate(s) ?: return null
            val d = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()
            return of(d.year, d.monthValue)
        }
    }

    object SavingsTable {
        val HEADER = listOf("Name", "Target", "Saved", "Deadline", "Icon", "Archived")
        /** Saved is informational: it is always recomputed from Saving transactions. */
        val NUMERIC_COLUMNS = setOf(1, 2)

        internal val TARGET = setOf("target", "muc tieu")
        private val NAME = setOf("name", "ten", "vault", "quy")
        private val DEADLINE = setOf("deadline", "han", "het han")
        private val ICON = setOf("icon", "bieu tuong")
        private val ARCHIVED = setOf("archived", "luu tru", "an")

        private val dateFmt = DateTimeFormatter.ofPattern("uuuu-MM-dd")

        fun toRows(items: List<VaultWithSaved>, neutralize: Boolean): List<List<String>> =
            listOf(HEADER) + items.map { (v, saved) ->
                listOf(
                    t(v.name, neutralize), v.target.toString(), saved.toString(),
                    v.deadline?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate().format(dateFmt) }.orEmpty(),
                    v.iconKey, if (v.archived) "Yes" else "No",
                )
            }

        class Columns(val name: Int, val target: Int, val deadline: Int, val icon: Int, val archived: Int)

        fun columns(first: List<String>) = Columns(find(first, NAME, 0), find(first, TARGET, 1), find(first, DEADLINE, 3), find(first, ICON, 4), find(first, ARCHIVED, 5))

        fun parseArchived(text: String) = normalize(text) in setOf("yes", "true", "1", "x", "co")

        /** The file keeps only the day, so the deadline becomes the start of that day. */
        fun parseDeadline(text: String): Long? = TransactionTable.parseDate(text)?.let {
            val day: LocalDate = Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
            day.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        }
    }
}
