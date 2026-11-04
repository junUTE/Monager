package com.monager.app.ui.model

import androidx.compose.runtime.Immutable
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.Tone

/** Row model for transaction lists (built from Room rows in the ViewModel). */
@Immutable
data class TxItem(val id: Long, val icon: MIcon, val tone: Tone, val title: String, val note: String, val date: String, val amount: Long)
