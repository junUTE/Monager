package com.monager.app.ui.settings

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
private val ThemeLabels = listOf("Hệ thống", "Sáng", "Tối")

@Composable
fun SettingsScreen(vm: SettingsViewModel, onBack: () -> Unit, onOpenCategories: () -> Unit) {
    val c = mc
    val ui by vm.ui.collectAsStateWithLifecycle()
    val theme by vm.themeMode.collectAsStateWithLifecycle()
    val stamp = LocalDate.now().toString()

    // System file picker (Storage Access Framework): no storage permission needed, works offline.
    val saveJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { it?.let { u -> vm.export(FileKind.JSON, u) } }
    val saveCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { it?.let { u -> vm.export(FileKind.CSV, u) } }
    val saveXlsx = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) { it?.let { u -> vm.export(FileKind.XLSX, u) } }
    val open = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { it?.let(vm::onFilePicked) }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "h") { ScreenHeader("Cài đặt", "Giao diện và dữ liệu", onBack = onBack) }

            ui.message?.let { msg ->
                item(key = "msg") {
                    AppCard {
                        Text(msg, color = c.text, fontSize = 12.sp)
                        Text("Đóng", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp).clickable(onClick = vm::dismissMessage))
                    }
                }
            }

            item(key = "t1") { SectionTitle("Giao diện") }
            item(key = "theme") {
                SegmentedTabs(ThemeLabels, Themes.indexOf(theme).coerceAtLeast(0), { vm.setTheme(Themes[it]) })
            }
            item(key = "cat") { Row3("Quản lý danh mục", "Thêm, sửa, sắp xếp danh mục", MIcon.Receipt, onClick = onOpenCategories) }

            item(key = "t2") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Sao lưu & xuất dữ liệu") } }
            item(key = "json") { Row3("Sao lưu đầy đủ (JSON)", "Giao dịch, danh mục, hạn mức, nợ, quỹ", MIcon.Wallet, enabled = !ui.busy) { saveJson.launch("monager-backup-$stamp.json") } }
            item(key = "csv") { Row3("Xuất giao dịch (CSV)", "Mở được bằng Excel, Google Sheets", MIcon.Receipt, enabled = !ui.busy) { saveCsv.launch("monager-giao-dich-$stamp.csv") } }
            item(key = "xlsx") { Row3("Xuất giao dịch (Excel .xlsx)", "File Excel chuẩn, số tiền là số", MIcon.Chart, enabled = !ui.busy) { saveXlsx.launch("monager-giao-dich-$stamp.xlsx") } }

            item(key = "t3") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Nhập dữ liệu") } }
            item(key = "import") {
                Row3("Nhập từ file", "JSON (khôi phục), CSV hoặc Excel (thêm giao dịch)", MIcon.Transfer, enabled = !ui.busy) { open.launch(arrayOf("*/*")) }
            }
            item(key = "hint") {
                Text(
                    "CSV/Excel cần các cột: Ngày, Loại, Số tiền, Danh mục, Danh mục con, Ví, Ví đến, Quỹ, Ghi chú. " +
                        "Nhập lại cùng một file sẽ không tạo giao dịch trùng. Tên danh mục/ví chưa có sẽ được tạo mới.",
                    color = c.muted, fontSize = 10.sp,
                )
            }

            item(key = "t4") { Box(Modifier.padding(top = 6.dp)) { SectionTitle("Thông tin") } }
            item(key = "info") {
                AppCard {
                    Text("100% ngoại tuyến", color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Dữ liệu chỉ nằm trên máy này, app không có quyền truy cập Internet.", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    Text("${ui.transactions} giao dịch · ${ui.dbSizeKb} KB", color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        if (ui.busy) {
            Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
                Text("Đang xử lý…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        ui.pendingRestore?.let { r ->
            SimpleSheet("Khôi phục từ bản sao lưu?", "Dữ liệu hiện tại sẽ bị thay thế hoàn toàn", onClose = vm::cancelRestore) {
                Text(
                    "File có ${r.transactions} giao dịch, ${r.categories} danh mục, ${r.wallets} ví, ${r.debts} khoản nợ. " +
                        "Mọi dữ liệu đang có trên máy sẽ bị xóa và thay bằng nội dung file. Hành động này không hoàn tác được.",
                    color = c.text, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp),
                )
                Row(Modifier.fillMaxWidth().padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, onClick = vm::cancelRestore) {
                        Text("Hủy", color = c.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    RaisedBox(Modifier.weight(1f).height(44.dp), radius = 13.dp, brush = c.heroBrush, onClick = vm::confirmRestore) {
                        Text("Khôi phục", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun Row3(title: String, subtitle: String, icon: MIcon, enabled: Boolean = true, onClick: () -> Unit) {
    val c = mc
    RaisedBox(Modifier.fillMaxWidth().alpha(if (enabled) 1f else .5f), radius = 16.dp, onClick = { if (enabled) onClick() }, contentAlignment = Alignment.CenterStart) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MIconView(icon, c.primary, size = 22.dp)
            Column(Modifier.weight(1f)) {
                Text(title, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = c.muted, fontSize = 10.sp)
            }
            MIconView(MIcon.ChevronRight, c.muted, size = 18.dp)
        }
    }
}
