package com.monager.app.ui.categories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.CategoryIcon
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.ScreenHeader
import com.monager.app.ui.components.SegmentedTabs
import com.monager.app.ui.components.raised
import com.monager.app.ui.sample.CategoryRow
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

private val Tabs = listOf("Chi tiêu", "Thu nhập")

@Composable
fun CategoriesScreen(onBack: () -> Unit) {
    val c = mc
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var expanded by rememberSaveable { mutableLongStateOf(1L) }
    var picker by rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val rows = if (tab == 0) SampleData.expenseCategories else SampleData.incomeCategories

    Box(Modifier.fillMaxSize().background(c.bg)) {
        LazyColumn(
            Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "h") {
                ScreenHeader("Danh mục", "Sắp xếp và tùy chỉnh", onBack = onBack, right = {
                    AppIconButton(MIcon.Plus, "Thêm danh mục", { picker = true }, c.primary)
                })
            }
            item(key = "tabs") { SegmentedTabs(Tabs, tab, { tab = it }) }
            item(key = "hint") {
                Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Nhấn giữ", color = c.muted, fontSize = 10.sp)
                    MIconView(MIcon.Drag, c.muted, size = 14.dp)
                    Text("để sắp xếp lại danh mục", color = c.muted, fontSize = 10.sp)
                }
            }
            items(rows, key = { it.id }, contentType = { "cat" }) { row ->
                CategoryGroup(row, row.id == expanded) { expanded = if (expanded == row.id) -1L else row.id }
            }
            item(key = "add") {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp)
                        .border(1.dp, c.primary, RoundedCornerShape(13.dp))
                        .clip(RoundedCornerShape(13.dp)).clickable { picker = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    MIconView(MIcon.Plus, c.primary, size = 18.dp)
                    Text("  Thêm danh mục", color = c.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (picker) IconPickerSheet(onClose = { picker = false })
    }
}

@Composable
private fun CategoryGroup(row: CategoryRow, open: Boolean, onToggle: () -> Unit) {
    val c = mc
    Column(Modifier.fillMaxWidth().raised(radius = 14.dp, depth = 3.dp)) {
        Row(
            Modifier.fillMaxWidth().height(57.dp).clickable(onClick = onToggle).padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MIconView(MIcon.Drag, c.muted.copy(alpha = .6f), size = 18.dp)
            CategoryIcon(row.icon, row.tone, size = 36.dp)
            Text(row.name, color = c.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            if (row.subs.isNotEmpty()) {
                MIconView(MIcon.ChevronRight, c.muted, Modifier.graphicsLayer { rotationZ = if (open) 90f else 0f }, 17.dp)
            }
            MIconView(MIcon.Edit, c.muted, size = 17.dp)
        }
        AnimatedVisibility(open && row.subs.isNotEmpty()) {
            Column {
                row.subs.forEach { sub ->
                    Row(
                        Modifier.fillMaxWidth().height(42.dp).padding(start = 47.dp, end = 13.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                    ) {
                        MIconView(MIcon.Drag, c.muted.copy(alpha = .6f), size = 16.dp)
                        Box(Modifier.size(6.dp).clip(CircleShape).background(c.primary))
                        Text(sub, color = c.text, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        MIconView(MIcon.Edit, c.muted, size = 16.dp)
                    }
                }
            }
        }
    }
}
