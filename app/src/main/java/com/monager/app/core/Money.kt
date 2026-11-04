package com.monager.app.core

import java.text.NumberFormat
import java.util.Locale

/** Money is stored as Long minor units (VND: whole dong). Never Double. */
object Money {
    // Built once; NumberFormat is expensive to construct.
    private val vnd: NumberFormat = NumberFormat.getInstance(Locale("vi", "VN"))
    fun plain(amount: Long): String = vnd.format(amount)
    fun format(amount: Long): String = vnd.format(amount) + " ₫"
}
