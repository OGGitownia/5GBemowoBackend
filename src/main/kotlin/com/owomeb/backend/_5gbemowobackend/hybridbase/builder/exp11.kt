package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream
import java.nio.charset.StandardCharsets

fun sanitize(text: String): String {
    val original = text
    var cleaned = text
    val log = mutableListOf<String>()

    if (cleaned.contains('\u00A0')) {
        cleaned = cleaned.replace('\u00A0', ' ')
        log.add("NBSP (U+00A0) → spacja")
    }
    if (cleaned.contains('\u200B')) {
        cleaned = cleaned.replace("\u200B", "")
        log.add("zero-width space (U+200B) → usunięty")
    }
    if (cleaned.contains('\t')) {
        cleaned = cleaned.replace("\t", " ")
        log.add("tabulacja (U+0009) → spacja")
    }
    if (cleaned.contains('\u201C')) {
        cleaned = cleaned.replace("\u201C", "\"")
        log.add("smart quote (U+201C) → \"")
    }
    if (cleaned.contains('\u201D')) {
        cleaned = cleaned.replace("\u201D", "\"")
        log.add("smart quote (U+201D) → \"")
    }
    if (Regex("\\s{2,}").containsMatchIn(cleaned)) {
        cleaned = cleaned.replace("\\s+".toRegex(), " ")
        log.add("wielokrotne spacje → pojedyncza")
    }

    cleaned = cleaned.trim()

    if (log.isNotEmpty()) {
        println("🧹 Oczyszczanie tekstu: \"$original\"")
        log.forEach { println(" - $it") }
        println("➡ Po oczyszczeniu: \"$cleaned\"")
    }

    return cleaned
}


fun main() {
    val inputPath = "src/main/resources/data/59/extractedNorm.docx"
    val outputPath = "src/main/resources/data/59/slimMarkdownHTMLUltraUTF8.md"

    val doc = XWPFDocument(FileInputStream(inputPath))
    val allElements = doc.bodyElements

    val forewordIndices = mutableListOf<Int>()
    for ((index, element) in allElements.withIndex()) {
        if (element is XWPFParagraph) {
            for (run in element.runs) {
                val runText = run.text()?.trim()
                if (runText != null && runText.equals("foreword", ignoreCase = true)) {
                    forewordIndices.add(index)
                    break
                }
            }
        }
    }

    if (forewordIndices.size != 2) {
        println("❌ Znaleziono ${forewordIndices.size} wystąpień słowa 'foreword'.")
        for ((index, element) in allElements.withIndex()) {
            if (element is XWPFParagraph) {
                for (run in element.runs) {
                    val runText = run.text()?.trim()
                    if (runText?.contains("foreword", ignoreCase = true) == true) {
                        println("• [index=$index] RUN='$runText', paragraphStyle='${element.style ?: "brak"}'")
                    }
                }
            }
        }
        throw IllegalStateException("Oczekiwano dokładnie 2 wystąpień słowa 'foreword'")
    }

    val secondIndex = forewordIndices[1]
    println("✅ Znaleziono dwa 'foreword'. Usuwam wszystko do indeksu $secondIndex włącznie.")
    for (i in secondIndex downTo 0) {
        doc.removeBodyElement(i)
    }

    val output = StringBuilder()
    for (element in doc.bodyElements) {
        when (element) {
            is XWPFParagraph -> {
                val style = element.style ?: ""
                val headingLevel = when {
                    style.startsWith("Heading", ignoreCase = true) ->
                        style.removePrefix("Heading").toIntOrNull()?.coerceIn(1, 6) ?: 0

                    else -> 0
                }

                val rawText = element.runs.joinToString("") { it.text() ?: "" }
                val cleanText = sanitize(rawText)

                if (cleanText.isNotEmpty()) {
                    if (headingLevel > 0) {
                        output.appendLine("#".repeat(headingLevel) + " " + cleanText)
                    } else {
                        output.appendLine(cleanText)
                    }
                }
            }

            is XWPFTable -> {
                val rows = element.rows
                if (rows.isNotEmpty()) {
                    output.appendLine("<table>")
                    for ((rowIndex, row) in rows.withIndex()) {
                        output.appendLine("  <tr>")
                        for (cell in row.tableCells) {
                            val cellText = sanitize(cell.text.replace("\\n", " "))
                            val tag = if (rowIndex == 0) "th" else "td"
                            output.appendLine("    <$tag>$cellText</$tag>")
                        }
                        output.appendLine("  </tr>")
                    }
                    output.appendLine("</table>")
                }
            }
        }
    }

    File(outputPath).writeText(output.toString().trim(), Charsets.UTF_8)
    println("✅ Zapisano oczyszczony markdown UTF-8 z tabelami HTML do: $outputPath")
}