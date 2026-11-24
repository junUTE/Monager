package com.monager.app.ui.settings

import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monager.app.core.CurrencyMark
import com.monager.app.core.MoneyFormat
import com.monager.app.core.NumberStyle
import com.monager.app.data.backup.BackupFormatException
import com.monager.app.data.backup.FileKind
import com.monager.app.data.backup.RestoreResult
import com.monager.app.data.backup.Snapshot
import com.monager.app.data.prefs.ThemeMode
import com.monager.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream

@Immutable
data class SettingsUi(
    val busy: Boolean = false,
    val message: String? = null,
    /** Set when a JSON backup was picked: restoring REPLACES everything, so the user must confirm. */
    val pendingRestore: RestoreResult? = null,
    val confirmReset: Boolean = false,
    val transactions: Int = 0,
    val dbSizeKb: Long = 0,
)

class SettingsViewModel(private val c: AppContainer) : ViewModel() {
    private val _ui = MutableStateFlow(SettingsUi())
    val ui: StateFlow<SettingsUi> = _ui.asStateFlow()

    val themeMode: StateFlow<ThemeMode> = c.settings.themeMode.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ThemeMode.SYSTEM)
    val moneyFormat: StateFlow<MoneyFormat> = c.settings.moneyFormat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MoneyFormat())
    private var pendingSnapshot: Snapshot? = null

    init { refreshInfo() }

    fun setTheme(mode: ThemeMode) { viewModelScope.launch { c.settings.setThemeMode(mode) } }
    fun setNumberStyle(style: NumberStyle) { viewModelScope.launch { c.settings.setNumberStyle(style) } }
    fun setCurrencyMark(mark: CurrencyMark) { viewModelScope.launch { c.settings.setCurrencyMark(mark) } }
    fun dismissMessage() = _ui.update { it.copy(message = null) }

    fun refreshInfo() {
        viewModelScope.launch {
            val counts = c.backup.counts()
            val size = withContext(Dispatchers.IO) { c.databaseFile().length() / 1024 }
            _ui.update { it.copy(transactions = counts.transactions, dbSizeKb = size) }
        }
    }

    fun export(kind: FileKind, uri: Uri) = run("Could not export file") {
        val out = c.contentResolver.openOutputStream(uri, "wt") ?: error("Could not open file for writing")
        out.use {
            when (kind) {
                FileKind.JSON -> c.backup.exportJson(it)
                FileKind.CSV -> c.backup.exportCsv(it)
                FileKind.XLSX -> c.backup.exportXlsx(it)
            }
        }
        "Export complete. You can open the file with ${if (kind == FileKind.JSON) "Monager itself (Import data)" else "Excel, Google Sheets or LibreOffice"}."
    }

    /** Called with the file the user picked in the system file picker. */
    fun onFilePicked(uri: Uri) = run("Could not read file") {
        val size = c.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { if (it.moveToFirst()) it.getLong(0) else -1L } ?: -1L
        if (size > MAX_IMPORT_BYTES) return@run "File is too large (max 25 MB)."
        val input = BufferedInputStream(c.contentResolver.openInputStream(uri) ?: error("Could not open file"))
        input.use { stream ->
            when (val kind = c.backup.detect(stream)) {
                FileKind.JSON -> {
                    val snap = c.backup.readSnapshot(stream)
                    pendingSnapshot = snap
                    _ui.update {
                        it.copy(pendingRestore = RestoreResult(snap.transactions.size, snap.categories.size, snap.wallets.size, snap.debts.size, snap.budgets.size, snap.vaults.size))
                    }
                    null
                }
                else -> {
                    val result = c.backup.importTables(c.backup.readTables(kind, stream))
                    buildString {
                        append("Imported ${result.imported} transactions")
                        if (result.duplicates > 0) append(", skipped ${result.duplicates} existing rows")
                        if (result.budgets > 0) append(", ${result.budgets} budgets")
                        if (result.savings > 0) append(", ${result.savings} savings goals")
                        if (result.createdCategories > 0) append(", created ${result.createdCategories} new categories")
                        append('.')
                        if (result.errors > 0) {
                            append("\n${result.errors} rows with errors were skipped:")
                            result.firstErrors.forEach { append("\n• $it") }
                            if (result.errors > result.firstErrors.size) append("\n• …")
                        }
                    }
                }
            }
        }
    }

    fun confirmRestore() {
        val snap = pendingSnapshot ?: return
        _ui.update { it.copy(pendingRestore = null) }
        pendingSnapshot = null
        run("Could not restore") {
            val r = c.backup.restore(snap)
            "Restored ${r.transactions} transactions, ${r.categories} categories, ${r.wallets} wallets, " +
                "${r.budgets} budgets, ${r.savings} savings goals, ${r.debts} debts."
        }
    }

    fun requestReset() = _ui.update { it.copy(confirmReset = true) }
    fun cancelReset() = _ui.update { it.copy(confirmReset = false) }

    fun confirmReset() {
        _ui.update { it.copy(confirmReset = false) }
        run("Could not reset") {
            c.backup.resetAll()
            "All data deleted and default categories restored."
        }
    }

    fun cancelRestore() { pendingSnapshot = null; _ui.update { it.copy(pendingRestore = null) } }

    /** Runs [block] with the busy flag; its returned text (if any) becomes the message, failures become [failure]. */
    private fun run(failure: String, block: suspend () -> String?) {
        viewModelScope.launch {
            _ui.update { it.copy(busy = true, message = null) }
            val msg = try {
                block()
            } catch (e: BackupFormatException) {
                e.message
            } catch (e: Exception) {
                "$failure: ${e.message ?: e.javaClass.simpleName}"
            }
            _ui.update { it.copy(busy = false, message = msg ?: it.message) }
            refreshInfo()
        }
    }

    private companion object { const val MAX_IMPORT_BYTES = 25L * 1024 * 1024 }
}
