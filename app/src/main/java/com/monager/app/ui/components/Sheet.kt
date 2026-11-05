package com.monager.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.ui.theme.mc

/** Number of [SimpleSheet]s currently open; the host hides the bottom navigation while it is > 0 so it cannot cover a sheet's buttons. */
val LocalOpenSheets = compositionLocalOf<MutableIntState?> { null }

/**
 * Lightweight bottom sheet: scrim + rounded Column. Avoids ModalBottomSheet's window and animation overhead.
 */
@Composable
fun SimpleSheet(title: String, subtitle: String? = null, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val c = mc
    val open = LocalOpenSheets.current
    BackHandler(onBack = onClose) // system Back closes the sheet instead of leaving the screen/app
    DisposableEffect(open) {
        open?.let { it.intValue++ }
        onDispose { open?.let { it.intValue-- } }
    }
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
                    Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 10.sp)
                }
                AppIconButton(MIcon.Close, "Đóng", onClose, c.text)
            }
            content()
        }
    }
}

/** Single-line text field in the app's inset style. [numeric] restricts input to digits via the keyboard type. */
@Composable
fun InsetField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, numeric: Boolean = false) {
    val c = mc
    Row(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.muted, fontSize = 13.sp)
            BasicTextField(
                value, onChange, singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                cursorBrush = SolidColor(c.primary),
                keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            )
        }
    }
}
