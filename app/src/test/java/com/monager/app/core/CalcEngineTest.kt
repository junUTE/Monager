package com.monager.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalcEngineTest {
    private fun ev(s: String) = CalcEngine.evaluate(s)

    @Test fun plainNumber() = assertEquals(250_000L, ev("250000"))
    @Test fun kSuffix() = assertEquals(80_000L, ev("50k+30k"))
    @Test fun addSubtract() = assertEquals(70L, ev("100-50+20"))
    @Test fun precedence() = assertEquals(14L, ev("2+3×4"))
    @Test fun precedenceDivide() = assertEquals(11L, ev("5+12÷2"))
    @Test fun leftToRightMulDiv() = assertEquals(6L, ev("12÷4×2"))
    @Test fun divisionRoundsHalfUp() { assertEquals(3L, ev("10÷3")); assertEquals(4L, ev("11÷3")); assertEquals(2L, ev("5÷2").let { it!! - 1 }) }
    @Test fun divideByZero() = assertNull(ev("5÷0"))
    @Test fun overflow() = assertNull(ev("999999999999×999999999999"))
    @Test fun malformed() { assertNull(ev("")); assertNull(ev("+5")); assertNull(ev("5+")); assertNull(ev("5++3")); assertNull(ev("abc")) }
    @Test fun asciiOperators() = assertEquals(20L, ev("4*5"))
    @Test fun negativeResultAllowedByEngine() = assertEquals(-5L, ev("5-10"))
}
