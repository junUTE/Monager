package com.monager.app.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.theme.mc

/** Number of [SimpleSheet]s currently open; the host hides the bottom navigation while it is > 0 so it cannot cover a sheet's buttons. */
val LocalOpenSheets = compositionLocalOf<MutableIntState?> { null }

/**
 * Lightweight bottom sheet: scrim + rounded Column. Avoids ModalBottomSheet's window and animation overhead.
 */
@Composable
fun SimpleSheet(title: String, subtitle: String? = null, onClose: () -> Unit, centered: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
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
        contentAlignment = if (centered) Alignment.Center else Alignment.BottomCenter,
    ) {
        val shape = if (centered) RoundedCornerShape(24.dp) else RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        val frame = if (centered) Modifier.statusBarsPadding().imePadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 24.dp) else Modifier
        Column(
            frame.fillMaxWidth().clip(shape).background(c.bg)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .then(if (centered) Modifier.verticalScroll(rememberScrollState()) else Modifier.imePadding().navigationBarsPadding())
                .padding(start = 16.dp, end = 16.dp, top = if (centered) 16.dp else 8.dp, bottom = 16.dp),
        ) {
            if (!centered) Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).clip(RoundedCornerShape(5.dp)).background(c.muted.copy(alpha = .5f)))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(title, color = c.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 10.sp)
                }
                AppIconButton(MIcon.Close, "Close", onClose, c.text)
            }
            content()
        }
    }
}

/** Scrollable grid of the app's icons (same set as categories). Fixed max height so a sheet never outgrows the screen. */
@Composable
fun IconPicker(selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    val c = mc
    Column(modifier.fillMaxWidth().heightIn(max = 150.dp).verticalScroll(rememberScrollState())) {
        MIcon.picker.chunked(6).forEach { rowIcons ->
            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                rowIcons.forEach { ic ->
                    val sel = ic.name == selected
                    Box(
                        Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(12.dp))
                            .background(if (sel) c.primarySoft else c.surface)
                            .border(if (sel) 1.5.dp else 1.dp, if (sel) c.primary else c.line, RoundedCornerShape(12.dp))
                            .clickable { onSelect(ic.name) },
                        contentAlignment = Alignment.Center,
                    ) { EmojiIcon(ic, if (sel) c.primary else c.muted) }
                }
                repeat(6 - rowIcons.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

/** Single-line text field in the app's inset style. [numeric] restricts input to digits via the keyboard type. */
@Composable
fun InsetField(value: String, onChange: (String) -> Unit, placeholder: String, modifier: Modifier = Modifier, numeric: Boolean = false, autoFocus: Boolean = false, money: Boolean = false) {
    val c = mc
    // The whole box is the touch target: an empty BasicTextField is only as wide as its text, so focus is requested explicitly.
    val focus = remember { androidx.compose.ui.focus.FocusRequester() }
    if (autoFocus) {
        val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
        androidx.compose.runtime.LaunchedEffect(Unit) {
            runCatching { focus.requestFocus() }
            keyboard?.show()
        }
    }
    // Local TextFieldValue so the whole prefilled amount can be selected on focus: the first digit typed replaces it.
    var tfv by remember { androidx.compose.runtime.mutableStateOf(androidx.compose.ui.text.input.TextFieldValue(value)) }
    if (tfv.text != value) tfv = androidx.compose.ui.text.input.TextFieldValue(value, androidx.compose.ui.text.TextRange(value.length))
    Row(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.requestFocus() }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f)) {
            if (value.isEmpty()) Text(placeholder, color = c.muted, fontSize = 13.sp)
            BasicTextField(
                tfv,
                { n -> tfv = n; if (n.text != value) onChange(n.text) },
                Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { f ->
                    if (f.isFocused && (money || numeric) && tfv.text.isNotEmpty()) tfv = tfv.copy(selection = androidx.compose.ui.text.TextRange(0, tfv.text.length))
                },
                singleLine = true,
                textStyle = TextStyle(color = c.text, fontSize = 14.sp),
                cursorBrush = SolidColor(c.primary),
                visualTransformation = if (money) MoneyTransformation else androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = if (numeric || money) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
            )
        }
        if (money) Text(Money.mark, color = c.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Shows a digits-only string with the thousands separator from the money setting; the stored text stays plain digits. */
private val MoneyTransformation = androidx.compose.ui.text.input.VisualTransformation { text ->
    val raw = text.text
    val shown = raw.toLongOrNull()?.let { Money.plain(it) } ?: raw
    val toShown = IntArray(raw.length + 1)
    var j = 0
    for (i in 0..raw.length) {
        // advance past separators so the cursor sits right after the i-th digit
        var digits = 0
        var k = 0
        while (k < shown.length && digits < i) { if (shown[k].isDigit()) digits++; k++ }
        toShown[i] = k
    }
    val toRaw = IntArray(shown.length + 1)
    var d = 0
    for (k in 0..shown.length) { toRaw[k] = d; if (k < shown.length && shown[k].isDigit()) d++ }
    androidx.compose.ui.text.input.TransformedText(
        androidx.compose.ui.text.AnnotatedString(shown),
        object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int) = toShown[offset.coerceIn(0, raw.length)]
            override fun transformedToOriginal(offset: Int) = toRaw[offset.coerceIn(0, shown.length)]
        },
    )
}
