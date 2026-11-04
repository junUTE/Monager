package com.monager.app.ui.analytics

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.monager.app.core.PeriodKind
import com.monager.app.core.Periods
import com.monager.app.data.local.entity.TxType
import com.monager.app.di.AppContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import kotlin.math.roundToInt

@Immutable
data class SliceUi(val name: String, val percent: Int, val amount: Long, val sweep: Float)

@Immutable
data class BarUi(val label: String, val income: Long, val expense: Long, val incomeFrac: Float, val expenseFrac: Float)

@Immutable
data class AnalyticsState(
    val loading: Boolean = true,
    val kind: PeriodKind = PeriodKind.MONTH,
    val label: String = "",
    val canGoNext: Boolean = false,
    val totalExpense: Long = 0,
    val totalIncome: Long = 0,
    /** Expense change vs the previous period in percent; null when the previous period had no expenses. */
    val changePct: Int? = null,
    val slices: List<SliceUi> = emptyList(),
    val bars: List<BarUi> = emptyList(),
)

private data class Request(val kind: PeriodKind, val offset: Int, val custom: Pair<LocalDate, LocalDate>?)

class AnalyticsViewModel(private val c: AppContainer) : ViewModel() {
    private val request = MutableStateFlow(Request(PeriodKind.MONTH, 0, null))

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<AnalyticsState> = request.flatMapLatest { req ->
        val today = LocalDate.now()
        val range = Periods.of(req.kind, req.offset, today, req.custom)
        val m = range.millis
        val prev = Periods.previous(range, today).millis
        combine(
            c.transactions.daily(m.from, m.to),
            c.transactions.totalsByParent(TxType.EXPENSE, m.from, m.to),
            c.categories.tree(TxType.EXPENSE),
            c.transactions.total(TxType.EXPENSE, prev.from, prev.to),
        ) { daily, groups, tree, prevExpense ->
            // All geometry/percent math happens here (Dispatchers.Default) so drawing only reads ready values.
            val income = daily.filter { it.type == TxType.INCOME }.associate { LocalDate.parse(it.day) to it.total }
            val expense = daily.filter { it.type == TxType.EXPENSE }.associate { LocalDate.parse(it.day) to it.total }
            val buckets = Periods.bucketize(range.buckets, income, expense)
            val peak = (buckets.maxOfOrNull { maxOf(it.second, it.third) } ?: 0L).coerceAtLeast(1L)
            val bars = buckets.map { (label, inc, exp) -> BarUi(label, inc, exp, inc.toFloat() / peak, exp.toFloat() / peak) }

            val totalExpense = groups.sumOf { it.total }
            val names = tree.associate { it.parent.id to it.parent.name }
            val named = groups.map { (names[it.groupId] ?: "Khác") to it.total }
            val top = named.take(MAX_SLICES - 1)
            val rest = named.drop(MAX_SLICES - 1).sumOf { it.second }
            val ranked = if (named.size > MAX_SLICES) top + ("Khác" to rest) else named
            val percents = ranked.map { if (totalExpense > 0) (it.second * 100.0 / totalExpense).roundToInt() else 0 }.toMutableList()
            if (percents.isNotEmpty()) percents[percents.lastIndex] = (100 - percents.dropLast(1).sum()).coerceAtLeast(0) // sums to exactly 100
            val slices = ranked.mapIndexed { i, (name, amount) ->
                SliceUi(name, percents[i], amount, if (totalExpense > 0) amount * 360f / totalExpense else 0f)
            }
            AnalyticsState(
                loading = false, kind = req.kind, label = range.label,
                canGoNext = req.kind != PeriodKind.CUSTOM && req.offset < 0,
                totalExpense = totalExpense, totalIncome = income.values.sum(),
                changePct = if (prevExpense > 0) ((totalExpense - prevExpense) * 100.0 / prevExpense).roundToInt() else null,
                slices = slices, bars = bars,
            )
        }
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsState())

    fun setKind(kind: PeriodKind) { request.value = Request(kind, 0, request.value.custom) }
    fun shift(delta: Int) {
        val r = request.value
        if (r.kind == PeriodKind.CUSTOM) return
        request.value = r.copy(offset = (r.offset + delta).coerceAtMost(0))
    }
    fun setCustom(from: LocalDate, to: LocalDate) { request.value = Request(PeriodKind.CUSTOM, 0, from to to) }

    private companion object { const val MAX_SLICES = 5 }
}
