package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

fun main() {
    val inputPath = "src/main/resources/data/59/extractedNorm.docx"
    val outputPath = "src/main/resources/data/59/slimMarkdownHTMLUltra.md"

    // Wczytaj dokument i usuń wszystko do drugiego foreworda (włącznie)
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
        println("🔍 Detale znalezionych elementów (max 10):")
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

    // Konwersja do markdown z tabelami HTML
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

                val text = element.runs.joinToString("") { it.text() ?: "" }.trim()

                if (text.isNotEmpty()) {
                    if (headingLevel > 0) {
                        output.appendLine("#".repeat(headingLevel) + " " + text)
                    } else {
                        output.appendLine(text)
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
                            val cellText = cell.text.trim().replace("\\s+".toRegex(), " ").replace("\n", " ")
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

    File(outputPath).writeText(output.toString().trim())
    println("Zapisano slim markdown z tabelami HTML do: $outputPath")
}