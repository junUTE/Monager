package com.monager.app.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TrendAxisTest {
    @Test fun stepsAreRoundNumbers() {
        assertEquals(100_000L, TrendAxis.step(350_000))
        assertEquals(50_000L, TrendAxis.step(120_000))
        assertEquals(1_000_000L, TrendAxis.step(3_800_000))
        assertEquals(1L, TrendAxis.step(0))
    }

    @Test fun axisAlwaysCoversMax() {
        for (max in listOf(1L, 7L, 99_999L, 100_000L, 350_000L, 8_320_000L, 123_456_789L)) {
            val step = TrendAxis.step(max)
            assertEquals(true, TrendAxis.steps(max, step) * step >= max)
        }
    }

    @Test fun labelsAreCompact() {
        assertEquals("0", TrendAxis.label(0))
        assertEquals("100k", TrendAxis.label(100_000))
        assertEquals("1.5M", TrendAxis.label(1_500_000))
        assertEquals("2M", TrendAxis.label(2_000_000))
        assertEquals("2B", TrendAxis.label(2_000_000_000))
    }
}
