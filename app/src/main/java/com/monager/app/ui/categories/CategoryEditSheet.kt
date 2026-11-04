package com.monager.app.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.data.local.entity.CategoryEntity
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.components.Tone
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

/**
 * Create / edit a category. [existing] == null means create ([parentName] != null: as its subcategory).
 * Lightweight sheet (Box + scrim) instead of ModalBottomSheet.
 */
@Composable
fun CategoryEditSheet(
    existing: CategoryEntity?,
    parentName: String?,
    onSave: (name: String, iconKey: String, toneKey: String) -> Unit,
    onDelete: (((Boolean) -> Unit) -> Unit)?,
    onMove: ((Int) -> Unit)?,
    onClose: () -> Unit,
) {
    val c = mc
    var name by rememberSaveable(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var icon by rememberSaveable(existing?.id) { mutableStateOf(existing?.iconKey ?: MIcon.Coffee.name) }
    var tone by rememberSaveable(existing?.id) { mutableStateOf(existing?.toneKey ?: Tone.Mint.name) }
    var error by remember { mutableStateOf<String?>(null) }
    val valid = name.trim().isNotEmpty()

    Box(
        Modifier.fillMaxSize().background(Color(0x85071510))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .imePadding().navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(c.muted.copy(alpha = .5f)))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(if (existing == null) "Thêm danh mục" else "Sửa danh mục", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (parentName != null) Text("Thuộc: $parentName", color = c.muted, fontSize = 10.sp)
                }
                AppIconButton(MIcon.Close, "Đóng", onClose, c.text)
            }

            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp).height(46.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    if (name.isEmpty()) Text("Tên danh mục", color = c.muted, fontSize = 13.sp)
                    BasicTextField(name, { if (it.length <= 30) name = it }, singleLine = true, textStyle = TextStyle(color = c.text, fontSize = 14.sp), cursorBrush = SolidColor(c.primary))
                }
            }

            Text("Màu", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tone.entries.forEach { t ->
                    val on = t.name == tone
                    Box(
                        Modifier.size(28.dp).clip(CircleShape).background(t.fg)
                            .then(if (on) Modifier.border(3.dp, c.text.copy(alpha = .85f), CircleShape) else Modifier)
                            .clickable { tone = t.name },
                    )
                }
            }

            Text("Biểu tượng", color = c.muted, fontSize = 10.sp, modifier = Modifier.padding(top = 12.dp, bottom = 6.dp))
            SampleData.pickerIcons.chunked(6).forEach { rowIcons ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowIcons.forEach { ic ->
                        val sel = ic.name == icon
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) c.primarySoft else c.surface)
                                .border(if (sel) 1.5.dp else 1.dp, if (sel) c.primary else c.line, RoundedCornerShape(12.dp))
                                .clickable { icon = ic.name },
                            contentAlignment = Alignment.Center,
                        ) { MIconView(ic, if (sel) c.primary else c.muted) }
                    }
                }
            }

            if (onMove != null) {
                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RaisedBox(Modifier.weight(1f).height(38.dp), radius = 10.dp, onClick = { onMove(-1) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MIconView(MIcon.ArrowUp, c.text, size = 16.dp); Text("Lên", color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    RaisedBox(Modifier.weight(1f).height(38.dp), radius = 10.dp, onClick = { onMove(1) }) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            MIconView(MIcon.ArrowDown, c.text, size = 16.dp); Text("Xuống", color = c.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            error?.let { Text(it, color = c.expense, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp)) }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    RaisedBox(Modifier.size(width = 52.dp, height = 44.dp), radius = 13.dp, onClick = {
                        onDelete { ok -> if (!ok) error = "Danh mục đã có giao dịch nên không thể xóa." }
                    }) { MIconView(MIcon.Delete, c.expense) }
                }
                RaisedBox(
                    Modifier.weight(1f).height(44.dp).alpha(if (valid) 1f else .5f), radius = 13.dp, brush = c.heroBrush,
                    onClick = { if (valid) onSave(name.trim(), icon, tone) },
                ) { Text("Lưu", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
