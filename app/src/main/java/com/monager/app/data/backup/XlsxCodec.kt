package com.monager.app.data.backup

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Tiny .xlsx reader/writer (no Apache POI: it would add several MB and slow startup).
 * Writes one sheet with inline strings; reads the first sheet of files saved by Excel, LibreOffice or Google Sheets
 * (shared strings, inline strings and numbers).
 */
object XlsxCodec {
    private const val MAX_ENTRY_BYTES = 40 * 1024 * 1024
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    /** [numericColumns] are written as real numbers (so Excel can sum them); everything else as text. */
    fun write(rows: List<List<String>>, numericColumns: Set<Int>, sheetName: String, out: OutputStream) {
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>""",
            )
            entry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            )
            entry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="$NS" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="${xml(sheetName)}" sheetId="1" r:id="rId1"/></sheets></workbook>""",
            )
            entry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>""",
            )
            zip.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            val w = zip.bufferedWriter(Charsets.UTF_8)
            w.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="$NS"><sheetData>""")
            rows.forEachIndexed { r, row ->
                w.write("""<row r="${r + 1}">""")
                row.forEachIndexed { c, cell ->
                    val ref = columnName(c) + (r + 1)
                    if (c in numericColumns && r > 0 && cell.toLongOrNull() != null) {
                        w.write("""<c r="$ref"><v>$cell</v></c>""")
                    } else if (cell.isNotEmpty()) {
                        w.write("""<c r="$ref" t="inlineStr"><is><t xml:space="preserve">${xml(cell)}</t></is></c>""")
                    }
                }
                w.write("</row>")
            }
            w.write("</sheetData></worksheet>")
            w.flush() // do not close: ZipOutputStream.use closes the stream
            zip.closeEntry()
        }
    }

    /** Returns rows of the first worksheet as strings (numbers keep Excel's raw text, e.g. "80000" or "46000.5"). */
    fun read(input: InputStream): List<List<String>> {
        var sharedXml: ByteArray? = null
        val sheets = sortedMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val name = e.name
                when {
                    name == "xl/sharedStrings.xml" -> sharedXml = readCapped(zip)
                    name.startsWith("xl/worksheets/") && name.endsWith(".xml") && !name.contains("/_rels/") -> sheets[name] = readCapped(zip)
                }
            }
        }
        require(sheets.isNotEmpty()) { "Không tìm thấy bảng tính trong file Excel" }
        val shared = sharedXml?.let { parseShared(it) } ?: emptyList()
        return parseSheet(sheets["xl/worksheets/sheet1.xml"] ?: sheets.values.first(), shared)
    }

    private fun readCapped(zip: ZipInputStream): ByteArray {
        val buf = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        while (true) {
            val n = zip.read(chunk)
            if (n < 0) break
            buf.write(chunk, 0, n)
            require(buf.size() <= MAX_ENTRY_BYTES) { "File Excel quá lớn" }
        }
        return buf.toByteArray()
    }

    private fun parseShared(bytes: ByteArray): List<String> {
        val p = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        val out = ArrayList<String>()
        val sb = StringBuilder()
        var inSi = false
        var inT = false
        var phoneticDepth = 0
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "si" -> { inSi = true; sb.setLength(0) }
                    "rPh" -> phoneticDepth++ // furigana text is not part of the value
                    "t" -> inT = inSi && phoneticDepth == 0
                }
                XmlPullParser.TEXT -> if (inT) sb.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "t" -> inT = false
                    "rPh" -> phoneticDepth--
                    "si" -> { out.add(sb.toString()); inSi = false }
                }
            }
            ev = p.next()
        }
        return out
    }

    private fun parseSheet(bytes: ByteArray, shared: List<String>): List<List<String>> {
        val p = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        val rows = ArrayList<List<String>>()
        var row = ArrayList<String>()
        var col = 0
        var type: String? = null
        var value = StringBuilder()
        var inV = false
        var inT = false
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            when (ev) {
                XmlPullParser.START_TAG -> when (p.name) {
                    "row" -> row = ArrayList()
                    "c" -> {
                        type = p.getAttributeValue(null, "t")
                        p.getAttributeValue(null, "r")?.let { col = columnIndex(it) }
                        value = StringBuilder()
                    }
                    "v" -> inV = true
                    "t" -> inT = true
                }
                XmlPullParser.TEXT -> if (inV || inT) value.append(p.text)
                XmlPullParser.END_TAG -> when (p.name) {
                    "v" -> inV = false
                    "t" -> inT = false
                    "c" -> {
                        while (row.size < col) row.add("")
                        val text = when (type) {
                            "s" -> shared.getOrNull(value.toString().trim().toIntOrNull() ?: -1).orEmpty()
                            "b" -> if (value.toString().trim() == "1") "TRUE" else "FALSE"
                            else -> value.toString()
                        }
                        if (row.size == col) row.add(text) else row[col] = text
                    }
                    "row" -> { if (row.any { it.isNotEmpty() }) rows.add(row) }
                }
            }
            ev = p.next()
        }
        return rows
    }

    fun columnName(index: Int): String {
        var n = index
        val sb = StringBuilder()
        do { sb.insert(0, 'A' + n % 26); n = n / 26 - 1 } while (n >= 0)
        return sb.toString()
    }

    /** "C5" -> 2, "AA1" -> 26. */
    fun columnIndex(ref: String): Int {
        var n = 0
        for (ch in ref) { if (!ch.isLetter()) break; n = n * 26 + (ch.uppercaseChar() - 'A' + 1) }
        return n - 1
    }

    private fun xml(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (ch in s) {
            when {
                ch == '&' -> sb.append("&amp;")
                ch == '<' -> sb.append("&lt;")
                ch == '>' -> sb.append("&gt;")
                ch == '"' -> sb.append("&quot;")
                ch.code < 0x20 && ch != '\n' && ch != '\r' && ch != '\t' -> Unit // illegal in XML 1.0
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }
}
