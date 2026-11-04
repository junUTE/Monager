package com.monager.app.data.backup

import android.util.JsonReader
import android.util.JsonToken
import android.util.JsonWriter
import com.monager.app.data.local.entity.BudgetEntity
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.data.local.entity.DebtEntity
import com.monager.app.data.local.entity.DebtPaymentEntity
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.data.local.entity.VaultEntity
import com.monager.app.data.local.entity.WalletEntity
import java.io.InputStream
import java.io.OutputStream

/** Everything in the database, as plain lists. */
data class Snapshot(
    val wallets: List<WalletEntity>,
    val categories: List<CategoryEntity>,
    val vaults: List<VaultEntity>,
    val transactions: List<TransactionEntity>,
    val budgets: List<BudgetEntity>,
    val debts: List<DebtEntity>,
    val debtPayments: List<DebtPaymentEntity>,
)

class BackupFormatException(message: String) : Exception(message)

/** Full-fidelity backup: streams JSON with android.util.JsonWriter/JsonReader (no extra dependency). */
object JsonBackup {
    const val APP = "monager"
    const val VERSION = 1

    fun write(snapshot: Snapshot, exportedAt: Long, out: OutputStream) {
        val w = JsonWriter(out.bufferedWriter(Charsets.UTF_8))
        w.setIndent("  ")
        w.beginObject()
        w.name("app").value(APP)
        w.name("version").value(VERSION.toLong())
        w.name("exportedAt").value(exportedAt)
        array(w, "wallets", snapshot.wallets) { it.long("id", id); it.str("name", name); it.long("initialBalance", initialBalance); it.str("iconKey", iconKey); it.bool("archived", archived); it.long("sortOrder", sortOrder.toLong()) }
        array(w, "categories", snapshot.categories) { it.long("id", id); it.str("name", name); it.str("type", type.name); it.long("parentId", parentId); it.str("iconKey", iconKey); it.str("toneKey", toneKey); it.long("sortOrder", sortOrder.toLong()) }
        array(w, "vaults", snapshot.vaults) { it.long("id", id); it.str("name", name); it.long("target", target); it.str("iconKey", iconKey); it.long("deadline", deadline); it.bool("archived", archived) }
        array(w, "transactions", snapshot.transactions) { it.long("id", id); it.long("walletId", walletId); it.str("type", type.name); it.long("amount", amount); it.long("categoryId", categoryId); it.long("vaultId", vaultId); it.long("toWalletId", toWalletId); it.str("note", note); it.long("timestamp", timestamp) }
        array(w, "budgets", snapshot.budgets) { it.long("id", id); it.long("categoryId", categoryId); it.long("month", month.toLong()); it.long("limitAmount", limitAmount) }
        array(w, "debts", snapshot.debts) { it.long("id", id); it.str("personName", personName); it.long("amount", amount); it.bool("iLent", iLent); it.long("dueDate", dueDate); it.str("note", note); it.long("createdAt", createdAt) }
        array(w, "debtPayments", snapshot.debtPayments) { it.long("id", id); it.long("debtId", debtId); it.long("amount", amount); it.long("timestamp", timestamp); it.long("transactionId", transactionId) }
        w.endObject()
        w.flush()
    }

    private fun <T> array(w: JsonWriter, name: String, items: List<T>, body: T.(JsonWriter) -> Unit) {
        w.name(name).beginArray()
        for (item in items) { w.beginObject(); item.body(w); w.endObject() }
        w.endArray()
    }

    private fun JsonWriter.str(n: String, v: String) { name(n).value(v) }
    private fun JsonWriter.long(n: String, v: Long?) { name(n); if (v == null) nullValue() else value(v) }
    private fun JsonWriter.bool(n: String, v: Boolean) { name(n).value(v) }

    /** Throws [BackupFormatException] for anything that is not a valid Monager backup of a supported version. */
    fun read(input: InputStream): Snapshot {
        try {
            val r = JsonReader(input.bufferedReader(Charsets.UTF_8))
            var app: String? = null
            var version = -1L
            val lists = HashMap<String, List<Map<String, Any?>>>()
            r.beginObject()
            while (r.hasNext()) {
                when (val key = r.nextName()) {
                    "app" -> app = r.nextString()
                    "version" -> version = r.nextLong()
                    "wallets", "categories", "vaults", "transactions", "budgets", "debts", "debtPayments" -> lists[key] = readArray(r)
                    else -> r.skipValue()
                }
            }
            r.endObject()
            if (app != APP) throw BackupFormatException("Đây không phải file sao lưu của Monager")
            if (version != VERSION.toLong()) throw BackupFormatException("Phiên bản sao lưu $version chưa được hỗ trợ")
            fun rows(name: String) = lists[name] ?: throw BackupFormatException("Thiếu phần \"$name\" trong file sao lưu")
            return Snapshot(
                wallets = rows("wallets").map { WalletEntity(it.l("id"), it.s("name"), it.l("initialBalance"), it.s("iconKey"), it.b("archived"), it.l("sortOrder").toInt()) },
                categories = rows("categories").map { CategoryEntity(it.l("id"), it.s("name"), it.type(), it.ln("parentId"), it.s("iconKey"), it.s("toneKey"), it.l("sortOrder").toInt()) },
                vaults = rows("vaults").map { VaultEntity(it.l("id"), it.s("name"), it.l("target"), it.s("iconKey"), it.ln("deadline"), it.b("archived")) },
                transactions = rows("transactions").map { TransactionEntity(it.l("id"), it.l("walletId"), it.type(), it.l("amount"), it.ln("categoryId"), it.ln("vaultId"), it.ln("toWalletId"), it.s("note"), it.l("timestamp")) },
                budgets = rows("budgets").map { BudgetEntity(it.l("id"), it.ln("categoryId"), it.l("month").toInt(), it.l("limitAmount")) },
                debts = rows("debts").map { DebtEntity(it.l("id"), it.s("personName"), it.l("amount"), it.b("iLent"), it.ln("dueDate"), it.s("note"), it.l("createdAt")) },
                debtPayments = rows("debtPayments").map { DebtPaymentEntity(it.l("id"), it.l("debtId"), it.l("amount"), it.l("timestamp"), it.ln("transactionId")) },
            )
        } catch (e: BackupFormatException) {
            throw e
        } catch (e: Exception) { // malformed JSON, wrong types, IO
            throw BackupFormatException("File sao lưu bị hỏng hoặc không đọc được (${e.message ?: e.javaClass.simpleName})")
        }
    }

    private fun readArray(r: JsonReader): List<Map<String, Any?>> {
        val list = ArrayList<Map<String, Any?>>()
        r.beginArray()
        while (r.hasNext()) {
            val m = HashMap<String, Any?>()
            r.beginObject()
            while (r.hasNext()) {
                val k = r.nextName()
                m[k] = when (r.peek()) {
                    JsonToken.NULL -> { r.nextNull(); null }
                    JsonToken.BOOLEAN -> r.nextBoolean()
                    JsonToken.NUMBER -> r.nextLong()
                    JsonToken.STRING -> r.nextString()
                    else -> { r.skipValue(); null }
                }
            }
            r.endObject()
            list.add(m)
        }
        r.endArray()
        return list
    }

    private fun Map<String, Any?>.l(k: String): Long = (this[k] as? Long) ?: throw BackupFormatException("Thiếu hoặc sai kiểu số: $k")
    private fun Map<String, Any?>.ln(k: String): Long? = this[k] as? Long
    private fun Map<String, Any?>.s(k: String): String = (this[k] as? String) ?: throw BackupFormatException("Thiếu hoặc sai kiểu chữ: $k")
    private fun Map<String, Any?>.b(k: String): Boolean = (this[k] as? Boolean) ?: throw BackupFormatException("Thiếu hoặc sai kiểu đúng/sai: $k")
    private fun Map<String, Any?>.type(): TxType =
        TxType.entries.firstOrNull { it.name == this["type"] } ?: throw BackupFormatException("Loại không hợp lệ: ${this["type"]}")
}
