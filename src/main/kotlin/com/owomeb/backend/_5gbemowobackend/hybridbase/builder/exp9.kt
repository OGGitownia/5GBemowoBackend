package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream

fun main() {
    val inputPath = "src/main/resources/data/59/normWithoutTableOfContent.docx"
    val outputPath = "src/main/resources/data/59/slimMarkdownHTML.md"

    val doc = XWPFDocument(FileInputStream(inputPath))
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
                            val cellText = cell.text
                                .trim()
                                .replace("\\s+".toRegex(), " ")
                                .replace("\n", " ")
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
    println("Zapisano slim markdown z tabelami w formacie HTML do: $outputPath")
}
