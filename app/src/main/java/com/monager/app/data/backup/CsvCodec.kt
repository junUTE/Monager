package com.monager.app.data.backup

import java.io.Writer

/**
 * Minimal RFC 4180 CSV. Writes UTF-8 with BOM so Excel shows Vietnamese correctly; reads comma or semicolon
 * separated files (Excel in Vietnamese locale saves with ';').
 */
object CsvCodec {
    private const val BOM = '﻿'

    fun write(rows: List<List<String>>, out: Writer) {
        out.write(BOM.code)
        for (row in rows) {
            row.forEachIndexed { i, cell ->
                if (i > 0) out.write(','.code)
                out.write(escape(cell))
            }
            out.write("\r\n")
        }
        out.flush()
    }

    private fun escape(cell: String): String {
        val needsQuote = cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' } ||
            cell.startsWith(' ') || cell.endsWith(' ')
        return if (needsQuote) "\"" + cell.replace("\"", "\"\"") + "\"" else cell
    }

    /** Spreadsheet apps execute cells that start with = + - @ as formulas; prefix such TEXT cells with an apostrophe. */
    fun neutralizeFormula(text: String): String =
        if (text.isNotEmpty() && text[0] in "=+-@\t\r") "'$text" else text

    fun restoreFormula(text: String): String =
        if (text.length >= 2 && text[0] == '\'' && text[1] in "=+-@\t\r") text.substring(1) else text

    fun parse(input: String): List<List<String>> {
        val text = input.removePrefix(BOM.toString())
        val delimiter = detectDelimiter(text)
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        val cell = StringBuilder()
        var inQuotes = false
        var i = 0
        fun endCell() { row.add(cell.toString()); cell.setLength(0) }
        fun endRow() {
            endCell()
            if (row.any { it.isNotEmpty() }) rows.add(row) // skip blank lines
            row = ArrayList()
        }
        while (i < text.length) {
            val ch = text[i]
            when {
                inQuotes -> when {
                    ch == '"' && i + 1 < text.length && text[i + 1] == '"' -> { cell.append('"'); i++ }
                    ch == '"' -> inQuotes = false
                    else -> cell.append(ch)
                }
                ch == '"' && cell.isEmpty() -> inQuotes = true
                ch == delimiter -> endCell()
                ch == '\r' -> { if (i + 1 < text.length && text[i + 1] == '\n') i++; endRow() }
                ch == '\n' -> endRow()
                else -> cell.append(ch)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) endRow()
        return rows
    }

    /** Picks ',' or ';' by counting them outside quotes on the first line. */
    private fun detectDelimiter(text: String): Char {
        var commas = 0
        var semis = 0
        var inQuotes = false
        for (ch in text) {
            if (ch == '"') inQuotes = !inQuotes
            else if (!inQuotes) {
                if (ch == '\n') break
                if (ch == ',') commas++ else if (ch == ';') semis++
            }
        }
        return if (semis > commas) ';' else ','
    }
}
