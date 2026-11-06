package com.monager.app.data.backup

import com.monager.app.data.local.dao.ExportRow
import com.monager.app.data.local.entity.TxType
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/** Shared by CSV and Excel: one row per transaction. Pure functions, no Android or DB. */
object TransactionTable {
    val HEADER = listOf("Date", "Type", "Amount", "Category", "Subcategory", "Wallet", "To wallet", "Vault", "Note")
    const val AMOUNT_COLUMN = 2

    private val dateFmt = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm")
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun label(type: TxType) = when (type) {
        TxType.EXPENSE -> "Expense"; TxType.INCOME -> "Income"; TxType.SAVING -> "Saving"; TxType.TRANSFER -> "Transfer"
    }

    /** [neutralize] true for CSV: text cells that look like formulas get an apostrophe prefix. */
    fun toRows(items: List<ExportRow>, neutralize: Boolean): List<List<String>> {
        fun t(s: String?) = if (s == null) "" else if (neutralize) com.monager.app.data.backup.CsvCodec.neutralizeFormula(s) else s
        return listOf(HEADER) + items.map {
            listOf(
                LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(it.timestamp), zone).format(dateFmt),
                label(it.type), it.amount.toString(), t(it.categoryName), t(it.parentName), t(it.walletName), t(it.toWalletName), t(it.vaultName), t(it.note),
            )
        }
    }

    // ---------- parsing ----------

    fun normalize(s: String): String =
        Normalizer.normalize(s.trim(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").replace('đ', 'd').replace('Đ', 'D').lowercase().replace(Regex("\\s+"), " ")

    fun parseType(text: String): TxType? = when (normalize(text)) {
        "chi tieu", "chi", "expense" -> TxType.EXPENSE
        "thu nhap", "thu", "income" -> TxType.INCOME
        "tiet kiem", "saving", "savings" -> TxType.SAVING
        "chuyen khoan", "transfer" -> TxType.TRANSFER
        else -> null
    }

    /** Whole VND from "80000", "80.000", "80,000", "80 000 ₫", "80000.0". Returns null when no positive amount can be read. */
    fun parseAmount(text: String): Long? {
        val t = text.filter { it.isDigit() || it == '.' || it == ',' }
        if (t.isEmpty()) return null
        val value = when {
            Regex("^\\d{1,3}([.,]\\d{3})+$").matches(t) -> t.replace(".", "").replace(",", "").toLongOrNull()
            Regex("^\\d+[.,]\\d{1,2}$").matches(t) -> runCatching { BigDecimal(t.replace(',', '.')).setScale(0, RoundingMode.HALF_UP).toLong() }.getOrNull()
            Regex("^\\d+$").matches(t) -> t.toLongOrNull()
            else -> null
        }
        return value?.takeIf { it > 0 }
    }

    // STRICT so that impossible dates such as 31/02/2026 are rejected instead of being "corrected" to 28/02.
    private fun strict(pattern: String) = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.STRICT)
    private val dateFormats = listOf(strict("uuuu-MM-dd HH:mm"), strict("uuuu-MM-dd'T'HH:mm"), strict("dd/MM/uuuu HH:mm"), strict("d/M/uuuu H:mm"))
    private val dateOnlyFormats = listOf(strict("uuuu-MM-dd"), strict("dd/MM/uuuu"), strict("d/M/uuuu"))

    /** Accepts our own format, dd/MM/yyyy (with or without time) and Excel serial numbers. Date-only values become 12:00. */
    fun parseDate(text: String): Long? {
        val s = text.trim()
        if (s.isEmpty()) return null
        for (f in dateFormats) try { return LocalDateTime.parse(s, f).atZone(zone).toInstant().toEpochMilli() } catch (_: DateTimeParseException) { }
        for (f in dateOnlyFormats) try { return LocalDate.parse(s, f).atTime(12, 0).atZone(zone).toInstant().toEpochMilli() } catch (_: DateTimeParseException) { }
        s.toDoubleOrNull()?.let { serial ->
            if (serial in 20_000.0..80_000.0) { // Excel's 1900 date system, 1899-12-30 as day 0
                val days = serial.toLong()
                val minutes = Math.round((serial - days) * 24 * 60)
                return LocalDate.of(1899, 12, 30).plusDays(days).atStartOfDay(zone).plusMinutes(minutes).toInstant().toEpochMilli()
            }
        }
        return null
    }

    /** Column positions, found from the header names when present (so reordered Excel files still import). */
    class Columns(val date: Int, val type: Int, val amount: Int, val category: Int, val sub: Int, val wallet: Int, val toWallet: Int, val vault: Int, val note: Int)

    private val names = mapOf(
        "date" to listOf("date", "ngay"), "type" to listOf("loai", "type"), "amount" to listOf("so tien", "amount"),
        "category" to listOf("danh muc", "category"), "sub" to listOf("danh muc con", "subcategory"),
        "wallet" to listOf("vi", "wallet"), "toWallet" to listOf("vi den", "to wallet"),
        "vault" to listOf("quy", "vault"), "note" to listOf("ghi chu", "note"),
    )

    /** Returns the column map and whether the first row was a header. */
    fun detectColumns(first: List<String>): Pair<Columns, Boolean> {
        val normalized = first.map { normalize(it) }
        fun find(key: String, default: Int) = normalized.indexOfFirst { it in names.getValue(key) }.let { if (it >= 0) it else default }
        val isHeader = normalized.any { n -> names.values.any { n in it } }
        if (!isHeader) return Columns(0, 1, 2, 3, 4, 5, 6, 7, 8) to false
        return Columns(find("date", 0), find("type", 1), find("amount", 2), find("category", 3), find("sub", 4), find("wallet", 5), find("toWallet", 6), find("vault", 7), find("note", 8)) to true
    }

    fun cell(row: List<String>, index: Int): String = row.getOrNull(index)?.trim().orEmpty()

}
