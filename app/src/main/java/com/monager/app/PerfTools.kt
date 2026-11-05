package com.monager.app

import android.app.Activity
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.metrics.performance.JankStats
import com.monager.app.data.local.entity.TransactionEntity
import com.monager.app.data.local.entity.TxType
import com.monager.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Random

/**
 * Measurement helpers. Everything is behind [BuildConfig.PERF_TOOLS], a compile-time constant that is
 * false in the real release build, so R8 removes this whole class (and JankStats) from shipped APKs.
 *
 *  - JankStats: logs janky frames under tag "MonagerJank" and a summary every 300 frames.
 *  - Demo data: launch with `--ez perf_seed true` to fill an EMPTY database with ~1500 transactions
 *    (used by the macrobenchmarks so lists actually scroll).
 */
object PerfTools {
    private const val TAG = "MonagerJank"
    const val EXTRA_SEED = "perf_seed"

    fun install(activity: Activity, container: AppContainer) {
        if (!BuildConfig.PERF_TOOLS) return
        trackJank(activity)
        if (activity.intent?.getBooleanExtra(EXTRA_SEED, false) == true && activity is androidx.activity.ComponentActivity) {
            activity.lifecycleScope.launch { withContext(Dispatchers.IO) { seed(container) } }
        }
    }

    private fun trackJank(activity: Activity) {
        var frames = 0
        var janky = 0
        var worstMs = 0L
        JankStats.createAndTrack(activity.window) { f ->
            frames++
            val ms = f.frameDurationUiNanos / 1_000_000
            if (ms > worstMs) worstMs = ms
            if (f.isJank) Log.w(TAG, "jank frame ${ms}ms")
            if (frames % 300 == 0) Log.i(TAG, "frames=$frames janky=$janky (${janky * 100 / frames}%) worstUi=${worstMs}ms")
            if (f.isJank) janky++
        }.isTrackingEnabled = true
    }

    private suspend fun seed(c: AppContainer) {
        if (c.transactions.recent(1).first().isNotEmpty()) return
        val wallet = c.wallets.wallets().first().firstOrNull() ?: return
        val expense = c.categories.tree(TxType.EXPENSE).first().map { it.parent.id }
        val income = c.categories.tree(TxType.INCOME).first().map { it.parent.id }
        if (expense.isEmpty() || income.isEmpty()) return
        val rnd = Random(42) // fixed seed: identical data on every run
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val notes = listOf("Cà phê", "Ăn trưa", "Grab", "Siêu thị", "Điện nước", "Tiền nhà", "Lương", "Thưởng", "")
        repeat(1500) { i ->
            val isIncome = i % 12 == 0
            c.transactions.add(
                TransactionEntity(
                    walletId = wallet.id,
                    type = if (isIncome) TxType.INCOME else TxType.EXPENSE,
                    amount = (if (isIncome) 5_000_000L else 20_000L) + rnd.nextInt(200) * 1_000L,
                    categoryId = (if (isIncome) income else expense)[rnd.nextInt(if (isIncome) income.size else expense.size)],
                    note = notes[rnd.nextInt(notes.size)],
                    timestamp = now - rnd.nextInt(90 * 24) * (day / 24) - rnd.nextInt(60) * 60_000L,
                ),
            )
        }
    }
}
