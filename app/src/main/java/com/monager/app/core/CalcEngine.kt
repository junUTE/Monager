package com.monager.app.core

/**
 * Pure Kotlin calculator for the amount field. Works on Long (whole dong), no Double.
 * Supports + - × ÷ (also * /), precedence (× ÷ before + -), and the "k" suffix (50k = 50.000).
 * Returns null for malformed input, divide-by-zero or overflow.
 */
object CalcEngine {
    fun evaluate(expression: String): Long? {
        val tokens = tokenize(expression) ?: return null
        if (tokens.isEmpty()) return null
        return try {
            // Pass 1: × ÷
            val terms = ArrayList<Long>()
            val ops = ArrayList<Char>()
            var acc = (tokens[0] as? Token.Num)?.value ?: return null
            var i = 1
            while (i < tokens.size) {
                val op = (tokens[i] as? Token.Op)?.symbol ?: return null
                val rhs = (tokens.getOrNull(i + 1) as? Token.Num)?.value ?: return null
                when (op) {
                    '×' -> acc = Math.multiplyExact(acc, rhs)
                    '÷' -> { if (rhs == 0L) return null; acc = divideRounded(acc, rhs) }
                    else -> { terms.add(acc); ops.add(op); acc = rhs }
                }
                i += 2
            }
            terms.add(acc)
            // Pass 2: + -
            var result = terms[0]
            for (k in ops.indices) {
                result = if (ops[k] == '+') Math.addExact(result, terms[k + 1]) else Math.subtractExact(result, terms[k + 1])
            }
            result
        } catch (_: ArithmeticException) {
            null
        }
    }

    /** Half-up rounding to whole dong. */
    private fun divideRounded(a: Long, b: Long): Long {
        val q = a / b
        val r = a % b
        return if (Math.abs(r) * 2 >= Math.abs(b)) q + (if ((a < 0) == (b < 0)) 1 else -1) else q
    }

    private sealed interface Token {
        data class Num(val value: Long) : Token
        data class Op(val symbol: Char) : Token
    }

    private fun tokenize(s: String): List<Token>? {
        val out = ArrayList<Token>()
        var i = 0
        while (i < s.length) {
            val ch = s[i]
            when {
                ch.isWhitespace() -> i++
                ch.isDigit() -> {
                    var v = 0L
                    while (i < s.length && s[i].isDigit()) {
                        v = try { Math.addExact(Math.multiplyExact(v, 10L), (s[i] - '0').toLong()) } catch (_: ArithmeticException) { return null }
                        i++
                    }
                    if (i < s.length && (s[i] == 'k' || s[i] == 'K')) {
                        v = try { Math.multiplyExact(v, 1000L) } catch (_: ArithmeticException) { return null }
                        i++
                    }
                    out.add(Token.Num(v))
                }
                ch == '+' || ch == '-' || ch == '−' -> { out.add(Token.Op(if (ch == '+') '+' else '-')); i++ }
                ch == '×' || ch == '*' || ch == 'x' -> { out.add(Token.Op('×')); i++ }
                ch == '÷' || ch == '/' -> { out.add(Token.Op('÷')); i++ }
                else -> return null
            }
        }
        return out
    }
}
