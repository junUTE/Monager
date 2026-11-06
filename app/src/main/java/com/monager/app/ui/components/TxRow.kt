package com.monager.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monager.app.core.Money
import com.monager.app.ui.model.TxItem
import com.monager.app.ui.theme.mc

/** Flat, cheap list row (no per-item shadow). Callers put it inside one raised card or on the screen background. */
@Composable
fun TransactionRow(t: TxItem, showDate: Boolean, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val c = mc
    val income = t.amount > 0
    Row(
        modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CategoryIcon(t.icon, t.tone, round = true)
        Column(Modifier.weight(1f)) {
            Text(t.title, color = c.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (t.note.isNotEmpty() || showDate) {
                Text(if (showDate) listOf(t.date, t.note).filter { it.isNotEmpty() }.joinToString(" · ") else t.note, color = c.muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text(
            (if (income) "+" else "−") + Money.format(kotlin.math.abs(t.amount)),
            color = if (income) c.income else c.expense, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        )
    }
}
