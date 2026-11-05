package com.monager.app.core

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.text.NumberFormat
import java.util.Locale

/** How thousands are separated: 1.234.567 (Vietnamese) or 1,234,567 (international). */
enum class NumberStyle(val label: String, val locale: Locale) {
    DOT("1.234.567", Locale.forLanguageTag("vi-VN")),
    COMMA("1,234,567", Locale.US),
}

/** Currency mark shown after the amount. Amounts stay whole dong; this only changes the label. */
enum class CurrencyMark(val label: String, val text: String) {
    DONG("₫", "₫"),
    SMALL_D("đ", "đ"),
    VND("VND", "VND"),
}

data class MoneyFormat(val style: NumberStyle = NumberStyle.DOT, val mark: CurrencyMark = CurrencyMark.DONG)

/**
 * Money is stored as Long minor units (VND: whole dong). Never Double.
 * The display format is Compose state, so any composable that calls [format] / [plain] / [mark]
 * recomposes by itself when the user changes the setting; nothing needs to be passed down.
 */
object Money {
    // Built once; NumberFormat is expensive to construct.
    private val formats = NumberStyle.entries.associateWith { NumberFormat.getInstance(it.locale) }

    private var current by mutableStateOf(MoneyFormat())

    fun configure(format: MoneyFormat) { current = format }

    val mark: String get() = current.mark.text

    fun plain(amount: Long): String = formats.getValue(current.style).format(amount)
    fun format(amount: Long): String = plain(amount) + " " + current.mark.text
}
