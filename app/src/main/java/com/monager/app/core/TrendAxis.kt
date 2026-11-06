package com.monager.app.core

import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/** "Nice" axis for the Home trend chart: grid lines at round amounts (100k, 200k ... / 1M, 2M ...). Pure, unit-tested. */
object TrendAxis {
    /** Smallest 1/2/5 x 10^k step that gives about four grid lines for [max]. */
    fun step(max: Long): Long {
        if (max <= 0) return 1
        val target = max / 4.0
        val mag = 10.0.pow(floor(log10(target.coerceAtLeast(1.0))))
        val norm = target / mag
        val nice = when {
            norm <= 1.0 -> 1.0
            norm <= 2.0 -> 2.0
            norm <= 5.0 -> 5.0
            else -> 10.0
        }
        return (nice * mag).toLong().coerceAtLeast(1)
    }

    /** Number of steps needed to cover [max] (>= 1). */
    fun steps(max: Long, step: Long): Int = ceil(max.toDouble() / step).toInt().coerceAtLeast(1)

    /** 0 -> "0", 100_000 -> "100k", 1_500_000 -> "1.5M", 2_000_000_000 -> "2B". */
    fun label(v: Long): String = when {
        v >= 1_000_000_000 -> trim(v / 1e9) + "B"
        v >= 1_000_000 -> trim(v / 1e6) + "M"
        v >= 1_000 -> trim(v / 1e3) + "k"
        else -> v.toString()
    }

    private fun trim(d: Double): String = String.format(Locale.ROOT, "%.1f", d).removeSuffix(".0")
}
