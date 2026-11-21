package com.monager.app.core

/**
 * Immutable state machine behind the in-app numpad. UI just calls [press] and renders [expression]/[result].
 * Keys: "0".."9", "000", "k" (x1000 suffix, once per number, after a digit), "+", "−", "×", "÷", "=", "C", "⌫".
 */
data class AmountInput(val expression: String = "") {

    /** Value shown big: result of the expression, ignoring a dangling operator. */
    val result: Long? get() = CalcEngine.evaluate(expression.trimEnd(*OPS))

    /** True when the expression has an operator, i.e. a secondary "50k + 30k" line is worth showing. */
    val hasOperator: Boolean get() = expression.any { it in OPS }

    /**
     * The expression as shown on screen: every number gets thousands separators via [plain] and operators are spaced,
     * e.g. "50000+30k" -> "50.000 + 30k". The "k" suffix stays as typed.
     */
    fun display(plain: (Long) -> String): String {
        val out = StringBuilder(expression.length + 8)
        var i = 0
        while (i < expression.length) {
            val ch = expression[i]
            when {
                ch.isDigit() -> {
                    var j = i
                    while (j < expression.length && expression[j].isDigit()) j++
                    val digits = expression.substring(i, j)
                    out.append(digits.toLongOrNull()?.let(plain) ?: digits)
                    i = j
                }
                ch in OPS -> { out.append(' ').append(ch).append(' '); i++ }
                else -> { out.append(ch); i++ }
            }
        }
        return out.toString()
    }

    fun press(key: String): AmountInput = when (key) {
        "C" -> AmountInput()
        "⌫" -> AmountInput(expression.dropLast(1))
        "=" -> result?.takeIf { it >= 0 }?.let { AmountInput(it.toString()) } ?: this
        "k" -> thousands()
        "+", "−", "×", "÷" -> operator(key[0])
        else -> digits(key)
    }

    private fun operator(op: Char): AmountInput {
        if (expression.isEmpty()) return this // cannot start with an operator
        val base = if (expression.last() in OPS) expression.dropLast(1) else expression // replace dangling op
        return AmountInput(base + op)
    }

    private fun thousands(): AmountInput {
        val lastNumber = expression.takeLastWhile { it !in OPS }
        if (lastNumber.isEmpty() || lastNumber.last() == 'k' || expression.length >= MAX_LEN) return this
        return AmountInput("${expression}k")
    }

    private fun digits(key: String): AmountInput {
        if (expression.length + key.length > MAX_LEN) return this
        val lastNumber = expression.takeLastWhile { it !in OPS }
        if (lastNumber.endsWith('k')) return this // digits after "k" would make an invalid number
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
