package com.monager.app.core

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

data class MillisRange(val from: Long, val to: Long)

object TimeRanges {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun month(ym: YearMonth): MillisRange = MillisRange(
        from = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli(),
        to = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1,
    )

    fun monthKey(ym: YearMonth): Int = ym.year * 100 + ym.monthValue

    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zone).toInstant().toEpochMilli()

    fun toDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** "Hôm nay", "Hôm qua", otherwise dd/MM. */
    fun label(millis: Long, today: LocalDate = LocalDate.now(zone)): String {
        val d = toDate(millis)
        return when (d) {
            today -> "Hôm nay"
            today.minusDays(1) -> "Hôm qua"
            else -> "%02d/%02d".format(d.dayOfMonth, d.monthValue)
        }
    }
}
