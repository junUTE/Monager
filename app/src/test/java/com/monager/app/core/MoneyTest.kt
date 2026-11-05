package com.monager.app.core

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test

class MoneyTest {
    @After fun reset() = Money.configure(MoneyFormat())

    @Test fun defaultIsVietnameseDotsAndDong() {
        assertEquals("1.234.567", Money.plain(1_234_567))
        assertEquals("1.234.567 ₫", Money.format(1_234_567))
    }

    @Test fun commaStyleUsesInternationalSeparators() {
        Money.configure(MoneyFormat(NumberStyle.COMMA, CurrencyMark.DONG))
        assertEquals("1,234,567 ₫", Money.format(1_234_567))
    }

    @Test fun currencyMarkCanBeChanged() {
        Money.configure(MoneyFormat(NumberStyle.DOT, CurrencyMark.VND))
        assertEquals("500.000 VND", Money.format(500_000))
        Money.configure(MoneyFormat(NumberStyle.DOT, CurrencyMark.SMALL_D))
        assertEquals("500.000 đ", Money.format(500_000))
        assertEquals("đ", Money.mark)
    }

    @Test fun zeroAndNegativeValues() {
        assertEquals("0 ₫", Money.format(0))
        assertEquals("-1.500 ₫", Money.format(-1_500))
    }
}
