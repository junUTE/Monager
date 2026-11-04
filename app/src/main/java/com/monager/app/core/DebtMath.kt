package com.monager.app.core

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

object DebtMath {
    /** Whole days past the due date (0 when not overdue, no due date, or already fully paid). */
    fun daysOverdue(dueMillis: Long?, remaining: Long, today: LocalDate = LocalDate.now()): Int {
        if (dueMillis == null || remaining <= 0) return 0
        val due = TimeRanges.toDate(dueMillis)
        return if (today.isAfter(due)) java.time.temporal.ChronoUnit.DAYS.between(due, today).toInt() else 0
    }

    /** Days until due (negative when overdue); null when there is no due date. */
    fun daysUntilDue(dueMillis: Long?, today: LocalDate = LocalDate.now()): Int? =
        dueMillis?.let { java.time.temporal.ChronoUnit.DAYS.between(today, TimeRanges.toDate(it)).toInt() }

    fun initials(name: String): String {
        val parts = name.trim().split(' ').filter { it.isNotEmpty() }
        return when (parts.size) {
            0 -> "?"
            1 -> parts[0].take(2).uppercase()
            else -> (parts.first().take(1) + parts.last().take(1)).uppercase()
        }
    }

    private val fmt = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)

    /** "15/11/2026" -> date; blank or invalid -> null. */
    fun parseDate(text: String): LocalDate? = try { LocalDate.parse(text.trim(), fmt) } catch (_: DateTimeParseException) { null }
    fun formatDate(date: LocalDate): String = date.format(fmt)
    fun formatDate(millis: Long): String = formatDate(TimeRanges.toDate(millis))
}
