package com.monager.app.core

/**
 * Immutable state machine behind the in-app numpad. UI just calls [press] and renders [expression]/[result].
 * Keys: "0".."9", "000", "+", "−", "×", "÷", "=", "C", "⌫".
 */
data class AmountInput(val expression: String = "") {

    /** Value shown big: result of the expression, ignoring a dangling operator. */
    val result: Long? get() = CalcEngine.evaluate(expression.trimEnd(*OPS))

    /** True when the expression has an operator, i.e. a secondary "50k + 30k" line is worth showing. */
    val hasOperator: Boolean get() = expression.any { it in OPS }

    fun press(key: String): AmountInput = when (key) {
        "C" -> AmountInput()
        "⌫" -> AmountInput(expression.dropLast(1))
        "=" -> result?.takeIf { it >= 0 }?.let { AmountInput(it.toString()) } ?: this
        "+", "−", "×", "÷" -> operator(key[0])
        else -> digits(key)
    }

    private fun operator(op: Char): AmountInput {
        if (expression.isEmpty()) return this // cannot start with an operator
        val base = if (expression.last() in OPS) expression.dropLast(1) else expression // replace dangling op
        return AmountInput(base + op)
    }

    private fun digits(key: String): AmountInput {
        if (expression.length + key.length > MAX_LEN) return this
        val lastNumber = expression.takeLastWhile { it !in OPS }
        if (lastNumber.length + key.length > MAX_DIGITS) return this
        // Avoid leading zeros: "0" + "5" -> "5", "0" + "000" -> "0"
        if (lastNumber == "0") {
            val trimmed = key.trimStart('0')
            return AmountInput(expression.dropLast(1) + (if (trimmed.isEmpty()) "0" else trimmed))
        }
        if (lastNumber.isEmpty() && key.all { it == '0' }) return AmountInput(expression + "0")
        return AmountInput(expression + key)
    }

    companion object {
        val OPS = charArrayOf('+', '−', '×', '÷')
        private const val MAX_LEN = 28
        private const val MAX_DIGITS = 12
    }
}
