package com.monager.app.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class PeriodsTest {
    private val today = LocalDate.of(2026, 11, 4) // Wednesday

    @Test fun weekStartsMondayAndHasSevenDays() {
        val r = Periods.of(PeriodKind.WEEK, 0, today)
        assertEquals(LocalDate.of(2026, 11, 2), r.from)
        assertEquals(LocalDate.of(2026, 11, 8), r.toInclusive)
        assertEquals(7, r.buckets.size)
        assertEquals("Mon", r.buckets.first().label)
        assertEquals("Sun", r.buckets.last().label)
    }

    @Test fun previousWeekOffset() = assertEquals(LocalDate.of(2026, 10, 26), Periods.of(PeriodKind.WEEK, -1, today).from)

    @Test fun monthBucketsCoverWholeMonthWithoutGaps() {
        val r = Periods.of(PeriodKind.MONTH, 0, today)
        assertEquals(LocalDate.of(2026, 11, 1), r.from)
        assertEquals(LocalDate.of(2026, 11, 30), r.toInclusive)
        assertEquals(5, r.buckets.size)
        assertEquals(LocalDate.of(2026, 11, 29), r.buckets.last().start)
        assertEquals(r.toInclusive, r.buckets.last().endInclusive)
        r.buckets.zipWithNext().forEach { (a, b) -> assertEquals(a.endInclusive.plusDays(1), b.start) }
    }

    @Test fun februaryNonLeapHasFourBuckets() = assertEquals(4, Periods.of(PeriodKind.MONTH, -9, today).buckets.size) // Feb 2026

    @Test fun yearHasTwelveMonthBuckets() {
        val r = Periods.of(PeriodKind.YEAR, -1, today)
        assertEquals(2025, r.from.year)
        assertEquals(12, r.buckets.size)
        assertEquals("T12", r.buckets.last().label)
    }

    @Test fun customPicksGranularityBySpan() {
        assertEquals(5, Periods.of(PeriodKind.CUSTOM, 0, today, today to today.plusDays(4)).buckets.size) // daily
        assertEquals(5, Periods.of(PeriodKind.CUSTOM, 0, today, today to today.plusDays(30)).buckets.size) // weekly
        assertEquals(4, Periods.of(PeriodKind.CUSTOM, 0, today, LocalDate.of(2026, 1, 15) to LocalDate.of(2026, 4, 20)).buckets.size) // monthly
    }

    @Test fun customSwapsReversedDates() {
        val r = Periods.of(PeriodKind.CUSTOM, 0, today, today to today.minusDays(3))
        assertTrue(r.from.isBefore(r.toInclusive))
        assertEquals(4, r.days)
    }

    @Test fun previousPeriodHasSameLengthAndEndsBeforeStart() {
        val cur = Periods.of(PeriodKind.CUSTOM, 0, today, LocalDate.of(2026, 11, 1) to LocalDate.of(2026, 11, 10))
        val prev = Periods.previous(cur, today)
        assertEquals(cur.days, prev.days)
        assertEquals(cur.from.minusDays(1), prev.toInclusive)
        assertEquals(LocalDate.of(2026, 10, 1), Periods.previous(Periods.of(PeriodKind.MONTH, 0, today), today).from)
        assertEquals(-2, Periods.offsetOf(Periods.of(PeriodKind.MONTH, -2, today), today))
    }

    @Test fun bucketizeSumsDaysIntoBuckets() {
        val r = Periods.of(PeriodKind.MONTH, 0, today)
        val out = Periods.bucketize(
            r.buckets,
            income = mapOf(LocalDate.of(2026, 11, 1) to 100L, LocalDate.of(2026, 11, 8) to 50L),
            expense = mapOf(LocalDate.of(2026, 11, 7) to 30L, LocalDate.of(2026, 11, 30) to 5L),
        )
        assertEquals(Triple("Week 1", 100L, 30L), out[0])
        assertEquals(Triple("Week 2", 50L, 0L), out[1])
        assertEquals(Triple("Week 5", 0L, 5L), out[4])
    }

    @Test fun millisRangeCoversWholeLastDay() {
        val r = Periods.of(PeriodKind.MONTH, 0, today).millis
        assertEquals(TimeRanges.startOfDay(LocalDate.of(2026, 12, 1)) - 1, r.to)
    }
}
