package com.monager.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.monager.app.core.DebtMath
import com.monager.app.ui.theme.mc
import java.time.LocalDate

/** Material date picker in a dialog. The picker speaks UTC-midnight millis; callers see a plain epoch day. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(epochDay: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = epochDay * 86_400_000L)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { state.selectedDateMillis?.let { onPick(Math.floorDiv(it, 86_400_000L)) }; onDismiss() }) { Text("Chọn") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    ) { DatePicker(state) }
}

/** 24h time picker in a dialog; value is minutes since midnight. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(minuteOfDay: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(initialHour = minuteOfDay / 60, initialMinute = minuteOfDay % 60, is24Hour = true)
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                TimePicker(state)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Hủy") }
                    TextButton(onClick = { onPick(state.hour * 60 + state.minute); onDismiss() }) { Text("Chọn") }
                }
            }
        }
    }
}

/**
 * Inset-style field that opens the date picker. The value is kept as dd/MM/yyyy text (empty = no date)
 * so existing screens keep their string state; [clearable] shows an x to empty it.
 */
@Composable
fun DateField(text: String, onChange: (String) -> Unit, placeholder: String, clearable: Boolean = false, modifier: Modifier = Modifier) {
    val c = mc
    var open by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(12.dp)).background(c.surface2)
            .clickable { open = true }.padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MIconView(MIcon.Calendar, c.primary, size = 18.dp)
        Text(if (text.isEmpty()) placeholder else text, color = if (text.isEmpty()) c.muted else c.text, fontSize = 14.sp, modifier = Modifier.weight(1f))
        if (clearable && text.isNotEmpty()) {
            Text("Xóa", color = c.expense, fontSize = 12.sp, modifier = Modifier.clickable { onChange("") }.padding(6.dp))
        }
    }
    if (open) {
        val initial = DebtMath.parseDate(text) ?: LocalDate.now()
        DatePickerSheet(initial.toEpochDay(), { onChange(DebtMath.formatDate(LocalDate.ofEpochDay(it))) }, { open = false })
    }
}
