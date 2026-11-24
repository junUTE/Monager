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
 * Writes worksheets with inline strings; reads every worksheet of files saved by Excel, LibreOffice or Google Sheets
 * (shared strings, inline strings and numbers).
 */
object XlsxCodec {
    private const val MAX_ENTRY_BYTES = 40 * 1024 * 1024
    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

    /** One worksheet: [numericColumns] are written as real numbers (so Excel can sum them); everything else as text. */
    class Sheet(val name: String, val rows: List<List<String>>, val numericColumns: Set<Int> = emptySet())

    fun write(rows: List<List<String>>, numericColumns: Set<Int>, sheetName: String, out: OutputStream) =
        write(listOf(Sheet(sheetName, rows, numericColumns)), out)

    /** Writes a workbook with one worksheet per entry of [sheets], in tab order. */
    fun write(sheets: List<Sheet>, out: OutputStream) {
        require(sheets.isNotEmpty())
        val numbers = sheets.indices.map { it + 1 }
        ZipOutputStream(out).use { zip ->
            fun entry(name: String, content: String) {
                zip.putNextEntry(ZipEntry(name))
                zip.write(content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entry(
                "[Content_Types].xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>""" +
                    numbers.joinToString("") { """<Override PartName="/xl/worksheets/sheet$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" } +
                    "</Types>",
            )
            entry(
                "_rels/.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            )
            entry(
                "xl/workbook.xml",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="$NS" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets>""" +
                    sheets.mapIndexed { i, sh -> """<sheet name="${xml(sh.name)}" sheetId="${i + 1}" r:id="rId${i + 1}"/>""" }.joinToString("") +
                    "</sheets></workbook>",
            )
            entry(
                "xl/_rels/workbook.xml.rels",
                """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">""" +
                    numbers.joinToString("") { """<Relationship Id="rId$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$it.xml"/>""" } +
                    "</Relationships>",
            )
            sheets.forEachIndexed { i, sheet ->
                zip.putNextEntry(ZipEntry("xl/worksheets/sheet${i + 1}.xml"))
                val w = zip.bufferedWriter(Charsets.UTF_8)
                w.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="$NS"><sheetData>""")
                sheet.rows.forEachIndexed { r, row ->
                    w.write("""<row r="${r + 1}">""")
                    row.forEachIndexed { c, cell ->
                        val ref = columnName(c) + (r + 1)
                        if (c in sheet.numericColumns && r > 0 && cell.toLongOrNull() != null) {
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
    }

    /** Rows of the first worksheet (in tab order) as strings. */
    fun read(input: InputStream): List<List<String>> = readAll(input).first()

    /** Rows of every worksheet in tab order (numbers keep Excel's raw text, e.g. "80000" or "46000.5"). */
    fun readAll(input: InputStream): List<List<List<String>>> {
        var sharedXml: ByteArray? = null
        var workbookXml: ByteArray? = null
        var relsXml: ByteArray? = null
        val sheets = HashMap<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            while (true) {
                val e = zip.nextEntry ?: break
                val name = e.name
                when {
                    name == "xl/sharedStrings.xml" -> sharedXml = readCapped(zip)
                    name == "xl/workbook.xml" -> workbookXml = readCapped(zip)
                    name == "xl/_rels/workbook.xml.rels" -> relsXml = readCapped(zip)
                    name.startsWith("xl/worksheets/") && name.endsWith(".xml") && !name.contains("/_rels/") -> sheets[name] = readCapped(zip)
                }
            }
        }
        require(sheets.isNotEmpty()) { "No worksheet found in the Excel file" }
        val shared = sharedXml?.let { parseShared(it) } ?: emptyList()
        val wb = workbookXml; val rels = relsXml
        val ordered = if (wb != null && rels != null) runCatching { sheetOrder(wb, rels) }.getOrDefault(emptyList()).filter { it in sheets } else emptyList()
        // Fallback when the workbook index is missing or unreadable: numeric file-name order (sheet2 before sheet10).
        val names = ordered.ifEmpty { sheets.keys.sortedWith(compareBy({ it.length }, { it })) }
        return names.map { parseSheet(sheets.getValue(it), shared) }
    }

    /** Worksheet zip paths in tab order, resolved through workbook.xml.rels. */
    private fun sheetOrder(workbook: ByteArray, rels: ByteArray): List<String> {
        val targets = HashMap<String, String>()
        forEachTag(rels, "Relationship") { attr ->
            val id = attr("Id"); val target = attr("Target")
            if (id != null && target != null) targets[id] = if (target.startsWith("/")) target.drop(1) else "xl/" + target.removePrefix("./")
        }
        val out = ArrayList<String>()
        forEachTag(workbook, "sheet") { attr -> (attr("r:id") ?: attr("id"))?.let { targets[it] }?.let { out.add(it) } }
        return out
    }

    private fun forEachTag(bytes: ByteArray, tag: String, body: (attr: (String) -> String?) -> Unit) {
        val p = Xml.newPullParser().apply { setInput(ByteArrayInputStream(bytes), "UTF-8") }
        var ev = p.eventType
        while (ev != XmlPullParser.END_DOCUMENT) {
            if (ev == XmlPullParser.START_TAG && p.name == tag) {
                body { name -> (0 until p.attributeCount).firstOrNull { p.getAttributeName(it) == name }?.let { p.getAttributeValue(it) } }
            }
            ev = p.next()
        }
    }

    private fun readCapped(zip: ZipInputStream): ByteArray {
        val buf = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(16 * 1024)
        while (true) {
            val n = zip.read(chunk)
            if (n < 0) break
            buf.write(chunk, 0, n)
            require(buf.size() <= MAX_ENTRY_BYTES) { "Excel file is too large" }
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
