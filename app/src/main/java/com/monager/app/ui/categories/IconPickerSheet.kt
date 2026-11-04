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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.ui.components.AppIconButton
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.MIconView
import com.monager.app.ui.components.RaisedBox
import com.monager.app.ui.sample.SampleData
import com.monager.app.ui.theme.mc

private val Filters = listOf("Tất cả", "Ăn uống", "Di chuyển", "Tiện ích")

/** Lightweight bottom sheet (plain Box + scrim): avoids the cost of ModalBottomSheet's window/animation machinery. */
@Composable
fun IconPickerSheet(onClose: () -> Unit) {
    val c = mc
    var chosen by rememberSaveable { mutableStateOf(MIcon.Coffee.name) }
    Box(
        Modifier.fillMaxSize().background(Color(0x85071510))
            .clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClose),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(c.bg)
                .clickable(interactionSource = MutableInteractionSource(), indication = null) {}
                .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 20.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(c.muted.copy(alpha = .5f)))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Chọn biểu tượng", color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Chọn biểu tượng phù hợp", color = c.muted, fontSize = 10.sp)
                }
                AppIconButton(MIcon.Close, "Đóng", onClose, c.text)
            }
            Row(Modifier.padding(vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Filters.forEachIndexed { i, f ->
                    val on = i == 0
                    Text(
                        f, color = if (on) c.onPrimary else c.muted, fontSize = 10.sp,
                        modifier = Modifier.clip(RoundedCornerShape(20.dp))
                            .background(if (on) c.primary else c.surface)
                            .border(1.dp, if (on) c.primary else c.line, RoundedCornerShape(20.dp))
                            .padding(horizontal = 11.dp, vertical = 7.dp),
                    )
                }
            }
            SampleData.pickerIcons.chunked(6).forEach { rowIcons ->
                Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowIcons.forEach { icon ->
                        val sel = icon.name == chosen
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                                .background(if (sel) c.primarySoft else c.surface)
                                .border(if (sel) 1.5.dp else 1.dp, if (sel) c.primary else c.line, RoundedCornerShape(12.dp))
                                .clickable { chosen = icon.name },
                            contentAlignment = Alignment.Center,
                        ) {
                            MIconView(icon, if (sel) c.primary else c.muted)
                            if (sel) {
                                Box(
                                    Modifier.align(Alignment.TopEnd).padding(2.dp).size(14.dp).clip(CircleShape).background(c.primary),
                                    contentAlignment = Alignment.Center,
                                ) { MIconView(MIcon.Check, c.onPrimary, size = 10.dp) }
                            }
                        }
                    }
                }
            }
            RaisedBox(Modifier.fillMaxWidth().padding(top = 8.dp).height(44.dp), radius = 13.dp, brush = c.heroBrush, onClick = onClose) {
                Text("Xác nhận", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
