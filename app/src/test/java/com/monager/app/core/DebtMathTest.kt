package com.monager.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class DebtMathTest {
    private val today = LocalDate.of(2026, 11, 4)
    private fun ms(y: Int, m: Int, d: Int) = TimeRanges.startOfDay(LocalDate.of(y, m, d))

    @Test fun notOverdueBeforeOrOnDueDate() {
        assertEquals(0, DebtMath.daysOverdue(ms(2026, 11, 5), 100, today))
        assertEquals(0, DebtMath.daysOverdue(ms(2026, 11, 4), 100, today))
    }
    @Test fun overdueCountsWholeDays() = assertEquals(3, DebtMath.daysOverdue(ms(2026, 11, 1), 100, today))
    @Test fun settledOrNoDueDateIsNeverOverdue() {
        assertEquals(0, DebtMath.daysOverdue(ms(2026, 1, 1), 0, today))
        assertEquals(0, DebtMath.daysOverdue(null, 100, today))
    }
    @Test fun daysUntilDue() {
        assertEquals(10, DebtMath.daysUntilDue(ms(2026, 11, 14), today))
        assertEquals(-2, DebtMath.daysUntilDue(ms(2026, 11, 2), today))
        assertNull(DebtMath.daysUntilDue(null, today))
    }
    @Test fun initials() {
        assertEquals("HN", DebtMath.initials("Hoàng Nam"))
        assertEquals("NL", DebtMath.initials("Nguyễn Ngọc Linh"))
        assertEquals("AN", DebtMath.initials("An"))
        assertEquals("?", DebtMath.initials("  "))
    }
    @Test fun parseDateIsStrict() {
        assertEquals(LocalDate.of(2026, 11, 15), DebtMath.parseDate("15/11/2026"))
        assertNull(DebtMath.parseDate("31/02/2026"))
        assertNull(DebtMath.parseDate("15-11-2026"))
        assertNull(DebtMath.parseDate(""))
        assertEquals("05/01/2026", DebtMath.formatDate(LocalDate.of(2026, 1, 5)))
    }
}
