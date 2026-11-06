package com.monager.app.ui.settings

import com.monager.app.BuildConfig
import androidx.compose.runtime.remember
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.monager.app.core.CurrencyMark
import com.monager.app.core.Money
import com.monager.app.core.NumberStyle
import com.monager.app.data.backup.FileKind
import com.monager.app.data.prefs.ThemeMode
import com.monager.app.ui.components.AppCard
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SectionTitle
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.SimpleSheet
import com.monager.app.ui.theme.mc
import java.time.LocalDate

private val Themes = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)
private val ThemeLabels = listOf("System", "Light", "Dark")
private val NumberStyles = NumberStyle.entries
private val CurrencyMarks = CurrencyMark.entries

@Composable
fun SettingsScreen(vm: SettingsViewModel, onOpenCategories: () -> Unit, onOpenDebts: () -> Unit, onOpenStats: () -> Unit) {
    val c = mc
    val ui by vm.ui.collectAsStateWithLifecycle()
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val stamp = LocalDate.now().toString()
    val releaseDate = remember { LocalDate.parse(BuildConfig.RELEASE_DATE).format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) }

    // System file picker (Storage Access Framework): no storage permission needed, works offline.
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { u -> vm.export(FileKind.JSON, u) } }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { u -> vm.export(FileKind.CSV, u) } }
    val saveXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { it?.let { u -> vm.export(FileKind.XLSX, u) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::onFilePicked) }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h") { ScreenHeader("Settings", "Appearance and data") }

            ui.message?.let { msg ->
                item(key = "msg") {
                    AppCard {
                        Text(msg, color = c.text, fontSize = 12.sp)
                        Text("Dismiss", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).clickable(onClick = vm::dismissMessage))
                    }
                }
            }

            item(key = "t0") { SectionTitle("Manage") }
            item(key = "stats") { Row3("Reports", "Cash flow, spending by category, comparisons", MIcon.Chart, onClick = onOpenStats) }
            item(key = "debts") { Row3("Debts", "Borrowed and lent, repaid over time", MIcon.Users, onClick = onOpenDebts) }
            item(key = "cat") { Row3("Manage categories", "Add, edit and reorder categories", MIcon.Receipt, onClick = onOpenCategories) }

            item(key = "t2") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Backup & export") } }
            item(key = "json") { Row3("Full backup (JSON)", "Transactions, categories, budgets, debts, savings goals", MIcon.Wallet, enabled = !ui.busy) { saveJson.launch("monager-backup-$stamp.json") } }
            item(key = "csv") { Row3("Export transactions (CSV)", "Opens in Excel, Google Sheets", MIcon.Receipt, enabled = !ui.busy) { saveCsv.launch("monager-transactions-$stamp.csv") } }
            item(key = "xlsx") { Row3("Export transactions (Excel .xlsx)", "Standard Excel file, amounts as numbers", MIcon.Chart, enabled = !ui.busy) { saveXlsx.launch("monager-transactions-$stamp.xlsx") } }

            item(key = "t3") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Import data") } }
            item(key = "import") {
                Row3("Import from file", "JSON (restore), CSV or Excel (adds transactions)", MIcon.Transfer, enabled = !ui.busy) { open.launch(arrayOf("*/*")) }
            }
            item(key = "hint") {
                Text(
                    "CSV/Excel needs these columns: Date, Type, Amount, Category, Subcategory, Wallet, To wallet, Vault, Note." +
                        "Importing the same file again will not create duplicate transactions. Missing category/wallet names will be created.",
                    color = c.muted, fontSize = 10.sp,
                )
            }

            item(key = "t1") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Preferences") } }
            item(key = "config", contentType = "config") {
                val fmt by vm.moneyFormat.collectAsStateWithLifecycle()
                AppCard {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SubTitle("Theme")
                        SegmentedTabs(ThemeLabels, Themes.indexOf(theme).coerceAtLeast(0), { vm.setTheme(Themes[it]) })
                        SubTitle("Money format")
                        Text("Thousands separator", color = c.muted, fontSize = 10.sp)
                        SegmentedTabs(NumberStyles.map { it.label }, NumberStyles.indexOf(fmt.style).coerceAtLeast(0), { vm.setNumberStyle(NumberStyles[it]) })
                        Text("Currency symbol", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
                        SegmentedTabs(CurrencyMarks.map { it.label }, CurrencyMarks.indexOf(fmt.mark).coerceAtLeast(0), { vm.setCurrencyMark(CurrencyMarks[it]) })
                        // Live preview: Money reads the saved format, so this line changes as soon as a tab is tapped.
                        Text("e.g. ${Money.format(1_234_567)}", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text("Only changes how amounts are displayed; data and exported files are unchanged.", color = c.muted, fontSize = 10.sp)
                    }
                }
            }
            item(key = "reset") {
                Row3(
                    "Reset data",
                    "Using ${sizeLabel(ui.dbSizeKb)} · ${ui.transactions} transactions. Deletes everything and restores default categories.",
                    MIcon.Delete, enabled = !ui.busy, tint = c.expense, onClick = vm::requestReset,
                )
            }

            item(key = "t4") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("About") } }
            item(key = "info") {
                AppCard {
                    InfoLine("App", "Monager")
                    InfoLine("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    InfoLine("Release date", releaseDate)
                    InfoLine("Language", "English")
                    InfoLine("Built with", "Kotlin · Jetpack Compose")
                }
            }
        }

        if (ui.busy) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                Text("Processing…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        if (ui.confirmReset) {
            SimpleSheet("Reset all data?", "This cannot be undone", onClose = vm::cancelReset) {
                Text(
                    "All transactions, budgets, debts, savings goals and custom categories will be deleted. " +
                        "The app returns to its initial state with a Cash wallet and the default categories. " +
                        "Back up (JSON) before resetting.",
                    color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = vm::cancelReset) {
                        Text("Keep data", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = vm::confirmReset) {
                        Text("Delete all", color = c.expense, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        ui.pendingRestore?.let { r ->
            SimpleSheet("Restore from backup?", "Current data will be completely replaced", onClose = vm::cancelRestore) {
                Text(
                    "The file has ${r.transactions} transactions, ${r.categories} categories, ${r.wallets} wallets, ${r.debts} debts. " +
                        "All data on this device will be deleted and replaced with the file contents. This cannot be undone.",
                    color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = vm::cancelRestore) {
                        Text("Cancel", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, brush = c.heroBrush, onClick = vm::confirmRestore) {
                        Text("Restore", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Row3(title: String, subtitle: String, icon: MIcon, enabled: Boolean = true, tint: Color? = null, onClick: () -> Unit) {
    val c = mc
    RaisedBox(Modifier.fillMaxWidth().alpha(if (enabled) 1f else .5f), radius = 16.dp, onClick = { if (enabled) onClick() }, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MIconView(icon, tint ?: c.primary, size = 22.dp)
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = c.muted, fontSize = 10.sp)
            }
            MIconView(MIcon.ChevronRight, c.muted, size = 18.dp)
        }
    }
}

@Composable
private fun SubTitle(text: String) {
    Text(text, color = mc.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun InfoLine(label: String, value: String) {
    val c = mc
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = c.muted, fontSize = 12.sp)
        Text(value, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun sizeLabel(kb: Long): String = if (kb >= 1024) "%.1f MB".format(kb / 1024.0) else "$kb KB"
