package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.apache.poi.xwpf.usermodel.XWPFTable
import java.io.File
import java.io.FileInputStream

fun main() {
    val inputPath = "src/main/resources/data/59/normWithoutTableOfContent.docx"
    val outputPath = "src/main/resources/data/59/slimMarkdown.md"

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
                    // Header
                    val headerCells = rows.first().tableCells.map { it.text.trim().replace("\\s+".toRegex(), " ") }
                    output.appendLine("| " + headerCells.joinToString(" | ") + " |")
                    output.appendLine("|" + headerCells.joinToString("|") { "---" } + "|")

                    // Body
                    for (row in rows.drop(1)) {
                        val cells = row.tableCells.map {
                            it.text.trim().replace("\\s+".toRegex(), " ").replace("\n", " ")
                        }
                        output.appendLine("| " + cells.joinToString(" | ") + " |")
                    }
                }
            }
        }
    }

    File(outputPath).writeText(output.toString().trim())
    println("✅ Zapisano slim markdown do: $outputPath")
}