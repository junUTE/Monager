package com.monager.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AmountInputTest {
    private fun keys(vararg k: String) = k.fold(AmountInput()) { s, key -> s.press(key) }

    @Test fun kSuffixMultipliesByThousand() = assertEquals(50_000L, keys("5", "0", "k").result)
    @Test fun kWorksInExpressions() = assertEquals(80_000L, keys("5", "0", "k", "+", "3", "0", "k").result)
    @Test fun kNeedsADigitFirst() { assertEquals("", keys("k").expression); assertEquals("5+", keys("5", "+", "k").expression) }
    @Test fun kOnlyOncePerNumber() = assertEquals("5k", keys("5", "k", "k").expression)
    @Test fun digitsAfterKAreIgnored() = assertEquals("5k", keys("5", "k", "3").expression)
    @Test fun typesDigits() = assertEquals(250L, keys("2", "5", "0").result)
    @Test fun tripleZero() = assertEquals(50_000L, keys("5", "0", "000").result)
    @Test fun noLeadingZeros() { assertEquals("5", keys("0", "5").expression); assertEquals("0", keys("0", "000").expression) }
    @Test fun cannotStartWithOperator() = assertEquals("", keys("+").expression)
    @Test fun operatorReplacesDangling() = assertEquals("5×", keys("5", "+", "×").expression)
    @Test fun danglingOperatorIgnoredInResult() = assertEquals(5L, keys("5", "+").result)

    @Test fun mockupExampleFiftyPlusThirty() {
        val s = keys("5", "0", "000", "+", "3", "0", "000")
        assertEquals(80_000L, s.result)
        assertTrue(s.hasOperator)
    }

    @Test fun equalsCollapsesExpression() {
        val s = keys("1", "0", "0", "−", "2", "5", "=")
        assertEquals("75", s.expression)
        assertFalse(s.hasOperator)
    }

    @Test fun equalsWithNegativeResultIsRejected() = assertEquals("5−10", keys("5", "−", "1", "0", "=").expression)
    @Test fun backspaceAndClear() { assertEquals("12", keys("1", "2", "3", "⌫").expression); assertEquals("", keys("1", "2", "C").expression) }
    @Test fun emptyHasNoResult() = assertNull(AmountInput().result)
    @Test fun capsDigitsPerNumber() = assertEquals(12, keys(*Array(15) { "9" }).expression.length)

    private val dots: (Long) -> String = { "%,d".format(java.util.Locale.US, it).replace(',', '.') }
    @Test fun displayGroupsThousandsInExpressions() =
        assertEquals("1.250.000 + 30k × 2", AmountInput("1250000+30k×2").display(dots))
    @Test fun displayKeepsDanglingOperatorAndSmallNumbers() = assertEquals("999 − ", AmountInput("999−").display(dots))
    @Test fun displayZero() = assertEquals("0", AmountInput("0").display(dots))
}
