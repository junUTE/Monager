package com.monager.app.domain.model

import androidx.compose.runtime.Immutable

/** UI-facing model: immutable so Compose can skip recomposition. */
@Immutable
data class TransactionUi(
    val id: Long,
    val title: String,
    val amountText: String,
    val isIncome: Boolean,
    val dateText: String,
)
