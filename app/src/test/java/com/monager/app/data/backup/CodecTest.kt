package com.monager.app.data.backup

import com.monager.app.data.local.entity.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CodecTest {
    private val tricky = listOf(
        listOf("Date", "Note"),
        listOf("2026-11-03 09:15", "cà phê, \"đặc biệt\""),
        listOf("2026-11-03 10:00", "dòng 1\ndòng 2"),
        listOf("", "  khoảng trắng  "),
    )

    private fun csv(rows: List<List<String>>) = StringWriter().also { CsvCodec.write(rows, it) }.toString()

    // ---------- CSV ----------
    @Test fun csvRoundTripKeepsQuotesCommasNewlinesAndVietnamese() = assertEquals(tricky, CsvCodec.parse(csv(tricky)))
    @Test fun csvStartsWithBomForExcel() = assertTrue(csv(tricky).startsWith("﻿"))
    @Test fun csvReadsSemicolonFilesFromVietnameseExcel() = assertEquals(listOf(listOf("a", "b"), listOf("1", "2,5")), CsvCodec.parse("a;b\r\n1;\"2,5\"\r\n"))
    @Test fun csvSkipsBlankLinesAndHandlesMissingFinalNewline() = assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), CsvCodec.parse("a,b\n\n1,2"))
    @Test fun csvHandlesWindowsAndUnixLineEndings() = assertEquals(3, CsvCodec.parse("a\r\nb\nc").size)

    @Test fun formulaCellsAreNeutralizedAndRestored() {
        for (danger in listOf("=SUM(A1)", "+1", "-2", "@x")) {
            val safe = CsvCodec.neutralizeFormula(danger)
            assertTrue(safe.startsWith("'"))
            assertEquals(danger, CsvCodec.restoreFormula(safe))
        }
        assertEquals("bình thường", CsvCodec.neutralizeFormula("bình thường"))
        assertEquals("'quote", CsvCodec.restoreFormula("'quote")) // a real leading apostrophe is left alone
    }

    // ---------- XLSX ----------
    @Test fun xlsxRoundTripWithSpecialCharacters() {
        val rows = listOf(
            TransactionTable.HEADER,
            listOf("2026-11-03 09:15", "Expense", "45000", "Food", "Coffee", "Cash", "", "", "<b>&\"'</b> note"),
            listOf("2026-11-04 10:00", "Income", "18000000", "Salary", "", "Cash", "", "", ""),
        )
        val out = ByteArrayOutputStream()
        XlsxCodec.write(rows, setOf(TransactionTable.AMOUNT_COLUMN), "Transactions", out)
        val back = XlsxCodec.read(ByteArrayInputStream(out.toByteArray()))
        assertEquals(rows.map { r -> r.map { it } }, back.map { r -> r + List(9 - r.size) { "" } })
    }

    @Test fun xlsxWritesAmountsAsNumbers() {
        val out = ByteArrayOutputStream()
        XlsxCodec.write(listOf(listOf("Amount"), listOf("80000")), setOf(0), "S", out)
        val sheet = java.util.zip.ZipInputStream(ByteArrayInputStream(out.toByteArray())).let { z ->
            generateSequence { z.nextEntry }.first { it.name == "xl/worksheets/sheet1.xml" }.let { z.readBytes().toString(Charsets.UTF_8) }
        }
        assertTrue(sheet, sheet.contains("<c r=\"A2\"><v>80000</v></c>"))
        assertTrue(sheet.contains("t=\"inlineStr\"")) // header stays text
    }

    @Test fun xlsxReadsExcelStyleSharedStringsNumbersAndGaps() {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { z ->
            z.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            z.write(
                ("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>
                <row r="1"><c r="A1" t="s"><v>0</v></c><c r="C1" t="s"><v>1</v></c></row>
                <row r="2"><c r="A2"><v>46000.5</v></c><c r="B2" t="inlineStr"><is><t>Chi</t></is></c><c r="C2"><v>80000</v></c></row>
                </sheetData></worksheet>""").toByteArray(),
            )
            z.closeEntry()
            // shared strings written AFTER the sheet on purpose (zip order must not matter); second string has rich text + phonetic
            z.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            z.write(
                ("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><t>Date</t></si>
                <si><r><t>Am</t></r><r><t>ount</t></r><rPh><t>furigana</t></rPh></si></sst>""").toByteArray(),
            )
            z.closeEntry()
        }
        val rows = XlsxCodec.read(ByteArrayInputStream(zip.toByteArray()))
        assertEquals(listOf("Date", "", "Amount"), rows[0])
        assertEquals(listOf("46000.5", "Chi", "80000"), rows[1])
    }

    @Test(expected = IllegalArgumentException::class) fun xlsxRejectsFilesWithoutWorksheet() {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { it.putNextEntry(ZipEntry("hello.txt")); it.write(1); it.closeEntry() }
        XlsxCodec.read(ByteArrayInputStream(zip.toByteArray()))
    }

    @Test fun columnNamesRoundTrip() {
        assertEquals("A", XlsxCodec.columnName(0)); assertEquals("Z", XlsxCodec.columnName(25)); assertEquals("AA", XlsxCodec.columnName(26))
        for (i in listOf(0, 8, 25, 26, 51, 701, 702)) assertEquals(i, XlsxCodec.columnIndex(XlsxCodec.columnName(i) + "7"))
    }

    // ---------- value parsing ----------
    @Test fun amountParsing() {
        assertEquals(80_000L, TransactionTable.parseAmount("80000"))
        assertEquals(80_000L, TransactionTable.parseAmount("80.000"))
        assertEquals(1_250_000L, TransactionTable.parseAmount("1,250,000 ₫"))
        assertEquals(80_000L, TransactionTable.parseAmount("80000.0"))
        assertEquals(2L, TransactionTable.parseAmount("1,5"))
        assertEquals(45_000L, TransactionTable.parseAmount("-45000"))
        assertNull(TransactionTable.parseAmount("")); assertNull(TransactionTable.parseAmount("abc")); assertNull(TransactionTable.parseAmount("0"))
    }

    @Test fun typeParsingIgnoresCaseAndAccents() {
        assertEquals(TxType.EXPENSE, TransactionTable.parseType("Expense")); assertEquals(TxType.EXPENSE, TransactionTable.parseType("Chi tiêu")); // legacy Vietnamese export assertEquals(TxType.EXPENSE, TransactionTable.parseType("CHI TIEU"))
        assertEquals(TxType.INCOME, TransactionTable.parseType("income")); assertEquals(TxType.SAVING, TransactionTable.parseType("Saving"))
        assertEquals(TxType.TRANSFER, TransactionTable.parseType("Transfer")); assertNull(TransactionTable.parseType("khác"))
    }

    @Test fun dateParsingFormatsAndExcelSerial() {
        val a = TransactionTable.parseDate("2026-11-03 09:15")!!
        assertEquals(a, TransactionTable.parseDate("03/11/2026 09:15"))
        assertEquals(TransactionTable.parseDate("2026-11-03")!!, TransactionTable.parseDate("03/11/2026")!!) // date-only -> 12:00
        val serial = TransactionTable.parseDate("46329.5")!! // 2026-11-03 12:00 in Excel's system
        assertEquals(TransactionTable.parseDate("2026-11-03 12:00"), serial)
        assertNull(TransactionTable.parseDate("31/02/2026")); assertNull(TransactionTable.parseDate("yesterday"))
    }

    @Test fun headerDetectionToleratesReorderedColumns() {
        val (cols, header) = TransactionTable.detectColumns(listOf("Amount", "Date", "Type", "Note"))
        assertTrue(header); assertEquals(0, cols.amount); assertEquals(1, cols.date); assertEquals(3, cols.note)
        assertTrue(!TransactionTable.detectColumns(listOf("2026-11-03 09:00", "Expense", "5000")).second)
    }
}
