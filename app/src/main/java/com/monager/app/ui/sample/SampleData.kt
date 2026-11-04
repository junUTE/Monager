package com.monager.app.ui.sample

import androidx.compose.runtime.Immutable
import com.monager.app.ui.components.MIcon
import com.monager.app.ui.components.Tone

/** Static UI models for the front-end phase. Replaced by ViewModel state backed by Room later. */
@Immutable data class TxItem(val id: Long, val icon: MIcon, val tone: Tone, val title: String, val note: String, val date: String, val amount: Long)
@Immutable data class BudgetItem(val id: Long, val icon: MIcon, val tone: Tone, val name: String, val used: Long, val max: Long) {
    val pct: Int get() = (used * 100 / max).toInt()
}
@Immutable data class DebtItem(val id: Long, val name: String, val initials: String, val tone: Tone, val total: Long, val remain: Long, val due: String)
@Immutable data class CategoryRow(val id: Long, val icon: MIcon, val tone: Tone, val name: String, val subs: List<String>)
@Immutable data class Slice(val name: String, val percent: Int, val amount: Long, val argb: Long)
@Immutable data class PickCategory(val icon: MIcon, val tone: Tone, val name: String)

object SampleData {
    val transactions = listOf(
        TxItem(1, MIcon.Utensils, Tone.Mint, "Ăn uống", "Bữa trưa", "Hôm nay", -85_000),
        TxItem(2, MIcon.Briefcase, Tone.Sage, "Lương tháng", "Techcombank", "25/10", 18_500_000),
        TxItem(3, MIcon.Car, Tone.Blue, "Di chuyển", "Grab", "24/10", -120_000),
        TxItem(4, MIcon.ShoppingBag, Tone.Peach, "Mua sắm", "Uniqlo", "23/10", -649_000),
    )
    val budgets = listOf(
        BudgetItem(1, MIcon.Utensils, Tone.Mint, "Ăn uống", 2_662_000, 3_500_000),
        BudgetItem(2, MIcon.Car, Tone.Blue, "Di chuyển", 1_498_000, 1_600_000),
        BudgetItem(3, MIcon.ShoppingBag, Tone.Peach, "Mua sắm", 2_163_000, 3_000_000),
    )
    val lend = listOf(
        DebtItem(1, "Hoàng Nam", "HN", Tone.Sage, 6_000_000, 4_500_000, "15/11/2026"),
        DebtItem(2, "Ngọc Linh", "NL", Tone.Peach, 3_200_000, 1_200_000, "28/10/2026"),
    )
    val borrow = listOf(DebtItem(3, "Anh Tuấn", "AT", Tone.Blue, 8_000_000, 5_000_000, "10/12/2026"))
    val expenseCategories = listOf(
        CategoryRow(1, MIcon.Utensils, Tone.Mint, "Ăn uống", listOf("Ăn sáng", "Ăn trưa & tối", "Cà phê")),
        CategoryRow(2, MIcon.Car, Tone.Blue, "Di chuyển", listOf("Xăng xe", "Taxi & Grab")),
        CategoryRow(3, MIcon.ShoppingBag, Tone.Peach, "Mua sắm", emptyList()),
        CategoryRow(4, MIcon.Receipt, Tone.Yellow, "Hóa đơn", emptyList()),
    )
    val incomeCategories = listOf(
        CategoryRow(11, MIcon.Briefcase, Tone.Sage, "Lương", listOf("Lương chính", "Thưởng")),
        CategoryRow(12, MIcon.Gift, Tone.Yellow, "Quà tặng", emptyList()),
    )
    val slices = listOf(
        Slice("Ăn uống", 32, 2_662_000, 0xFF143D30),
        Slice("Mua sắm", 26, 2_163_000, 0xFFDC604E),
        Slice("Di chuyển", 18, 1_498_000, 0xFF557A9B),
        Slice("Hóa đơn", 14, 1_165_000, 0xFFD29D3F),
        Slice("Khác", 10, 832_000, 0xFFAAB3AE),
    )
    val weekBars = listOf(44f to 64f, 74f to 46f, 52f to 78f, 80f to 56f) // income to expense, dp
    val pickCategories = listOf(
        PickCategory(MIcon.Utensils, Tone.Mint, "Ăn uống"),
        PickCategory(MIcon.Car, Tone.Blue, "Di chuyển"),
        PickCategory(MIcon.ShoppingBag, Tone.Peach, "Mua sắm"),
        PickCategory(MIcon.Receipt, Tone.Yellow, "Hóa đơn"),
        PickCategory(MIcon.Heart, Tone.Purple, "Sức khỏe"),
        PickCategory(MIcon.More, Tone.Gray, "Khác"),
    )
    val pickerIcons = listOf(
        MIcon.Utensils, MIcon.Coffee, MIcon.Car, MIcon.Plane, MIcon.ShoppingBag, MIcon.Receipt,
        MIcon.Phone, MIcon.House, MIcon.Heart, MIcon.Graduation, MIcon.Gift, MIcon.Briefcase,
    )
}
