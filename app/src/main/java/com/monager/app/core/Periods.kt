package com.monager.app.core

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class PeriodKind { WEEK, MONTH, YEAR, CUSTOM }

data class Bucket(val start: LocalDate, val endInclusive: LocalDate, val label: String)

data class PeriodRange(val kind: PeriodKind, val from: LocalDate, val toInclusive: LocalDate, val label: String, val buckets: List<Bucket>) {
    val millis: MillisRange
        get() = MillisRange(TimeRanges.startOfDay(from), TimeRanges.startOfDay(toInclusive.plusDays(1)) - 1)
    val days: Int get() = ChronoUnit.DAYS.between(from, toInclusive).toInt() + 1
}

/** Pure date math for the Analytics screen (no Android, easy to test). offset 0 = current, -1 = previous, ... */
object Periods {
    private val weekDayLabels = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")

    fun of(kind: PeriodKind, offset: Int, today: LocalDate, custom: Pair<LocalDate, LocalDate>? = null): PeriodRange = when (kind) {
        PeriodKind.WEEK -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).plusWeeks(offset.toLong())
            val end = start.plusDays(6)
            PeriodRange(kind, start, end, "${fmt(start)} – ${fmt(end)}", (0..6).map { Bucket(start.plusDays(it.toLong()), start.plusDays(it.toLong()), weekDayLabels[it]) })
        }
        PeriodKind.MONTH -> {
            val ym = YearMonth.from(today).plusMonths(offset.toLong())
            val start = ym.atDay(1)
            val end = ym.atEndOfMonth()
            val buckets = (0 until 5).mapNotNull { i ->
                val s = start.plusDays(i * 7L)
                if (s.isAfter(end)) null else Bucket(s, minOf(s.plusDays(6), end), "Tuần ${i + 1}")
            }
            PeriodRange(kind, start, end, "Tháng ${ym.monthValue}/${ym.year}", buckets)
        }
        PeriodKind.YEAR -> {
            val year = today.year + offset
            val buckets = (1..12).map { m -> YearMonth.of(year, m).let { Bucket(it.atDay(1), it.atEndOfMonth(), "T$m") } }
            PeriodRange(kind, LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31), "Năm $year", buckets)
        }
        PeriodKind.CUSTOM -> {
            val (a, b) = custom ?: (today.minusDays(29) to today)
            val from = minOf(a, b)
            val to = maxOf(a, b)
            PeriodRange(kind, from, to, "${fmt(from)} – ${fmt(to)}", customBuckets(from, to))
        }
    }

    /** The period of equal length right before [r]; used for the "vs previous period" comparison. */
    fun previous(r: PeriodRange, today: LocalDate): PeriodRange = when (r.kind) {
        PeriodKind.CUSTOM -> {
            val len = r.days.toLong()
            of(PeriodKind.CUSTOM, 0, today, r.from.minusDays(len) to r.from.minusDays(1))
        }
        else -> of(r.kind, offsetOf(r, today) - 1, today)
    }

    fun offsetOf(r: PeriodRange, today: LocalDate): Int = when (r.kind) {
        PeriodKind.WEEK -> ChronoUnit.WEEKS.between(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)), r.from).toInt()
        PeriodKind.MONTH -> ChronoUnit.MONTHS.between(YearMonth.from(today), YearMonth.from(r.from)).toInt()
        PeriodKind.YEAR -> r.from.year - today.year
        PeriodKind.CUSTOM -> 0
    }

    private fun customBuckets(from: LocalDate, to: LocalDate): List<Bucket> {
        val days = ChronoUnit.DAYS.between(from, to).toInt() + 1
        return when {
            days <= 14 -> (0 until days).map { from.plusDays(it.toLong()).let { d -> Bucket(d, d, fmt(d)) } }
            days <= 92 -> generateSequence(from) { it.plusDays(7) }.takeWhile { !it.isAfter(to) }
                .map { s -> Bucket(s, minOf(s.plusDays(6), to), fmt(s)) }.toList()
            else -> generateSequence(YearMonth.from(from)) { it.plusMonths(1) }.takeWhile { !it.isAfter(YearMonth.from(to)) }
                .map { ym -> Bucket(maxOf(ym.atDay(1), from), minOf(ym.atEndOfMonth(), to), "T${ym.monthValue}") }.toList()
        }
    }

    private fun fmt(d: LocalDate) = "%02d/%02d".format(d.dayOfMonth, d.monthValue)

    /** Sums per-day totals into [buckets]. Days outside every bucket are ignored. Returns (label, income, expense). */
    fun bucketize(buckets: List<Bucket>, income: Map<LocalDate, Long>, expense: Map<LocalDate, Long>): List<Triple<String, Long, Long>> =
        buckets.map { b ->
            var inc = 0L
            var exp = 0L
            var d = b.start
            while (!d.isAfter(b.endInclusive)) {
                inc += income[d] ?: 0L
                exp += expense[d] ?: 0L
                d = d.plusDays(1)
            }
            Triple(b.label, inc, exp)
        }
}
