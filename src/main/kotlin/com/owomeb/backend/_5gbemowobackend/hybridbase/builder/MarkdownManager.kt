package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.Paragraph
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.springframework.stereotype.Component
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

@Component
class MarkdownManager {

    fun isMarkdownExists(markdownPath: String): Boolean {
        return File(markdownPath).exists()
    }

    fun convertDocToMarkdown(docPath: String, markdownPath: String, pureMarkdownPath: String): Boolean {
        return try {
            val file = File(docPath)
            if (!file.exists()) {
                println("Błąd: Plik DOC nie istnieje.")
                return false
            }

            val markdownContent = when {
                docPath.endsWith(".docx") -> processDocxToMarkdown(XWPFDocument(FileInputStream(file)))
                docPath.endsWith(".doc") -> processDocToMarkdown(HWPFDocument(FileInputStream(file)))

                else -> {
                    println("Błąd: Nieobsługiwany format pliku: $docPath")
                    return false
                }
            }


            FileOutputStream(markdownPath).use { output ->
                output.write(markdownContent.toByteArray())
            }
            println("Markdown utworzony: $markdownPath")
            cleanMarkdown(markdownPath, pureMarkdownPath)

            true
        } catch (e: Exception) {
            println("Błąd podczas konwersji DOC na Markdown: ${e.message}")
            false
        }
    }


    fun cleanMarkdown(markdownPath: String, clearMarkdownPath: String) {
        val inputFile = File(markdownPath)
        val outputFile = File(clearMarkdownPath)

        if (!inputFile.exists()) {
            println("Plik źródłowy nie istnieje: $markdownPath")
            return
        }

        val cleanedLines = mutableListOf<String>()
        var isInTableOfContents = false
        var isBeforeMainContent = true
        val removedLines = mutableListOf<String>()

        inputFile.forEachLine { line ->
            val trimmedLine = line.trim()

            // poniżej próba usnięcia spisu treści działała dobrze dla LTE RRC
            if (trimmedLine.matches(Regex("(?i).*table of contents.*")) || trimmedLine.matches(Regex("(?i).*contents.*"))) {
                isInTableOfContents = true
                removedLines.add("Usunięto spis treści: $line")
                return@forEachLine
            }
            if (isInTableOfContents && trimmedLine.isEmpty()) {
                isInTableOfContents = false
                return@forEachLine
            }
            if (isInTableOfContents) {
                removedLines.add("Usunięto spis treści: $line")
                return@forEachLine
            }

            // Usuwaniemetadanych na początku dokumentu
            if (isBeforeMainContent) {
                if (trimmedLine.matches(Regex("^(?i)(source|date of version|3GPP TS|ETSI|.*project.*|.*trade mark.*|©).*"))) {
                    removedLines.add("Usunięto metadane/nagłówek: $line")
                    return@forEachLine
                }
                // Jeśli znajdziemy pierwszą rzeczywistą treść dokumentu, kończymy czyszczenie
                if (trimmedLine.matches(Regex("^\\d+(\\.\\d+)*\\s+.*"))) {
                    isBeforeMainContent = false
                }
            }
            if (trimmedLine.matches(Regex("^\\d+(\\.\\d+)*\\s+.*"))) {
                removedLines.add("Usunięto spis treści: $line")
                return@forEachLine
            }

            cleanedLines.add(line)
        }

        outputFile.writeText(cleanedLines.joinToString("\n"))
        println("Markdown oczyszczony i zapisany do: $clearMarkdownPath")

        if (removedLines.isNotEmpty()) {
            println("Usunięto linie:")
            removedLines.forEach { println(it) }
        }
    }



    private fun processDocxToMarkdown(document: XWPFDocument): String {
        val markdown = StringBuilder()

        for (paragraph in document.paragraphs) {
            when {
                paragraph.style?.startsWith("Heading") == true -> {
                    val level = paragraph.style.lastOrNull()?.digitToIntOrNull() ?: 1
                    markdown.append("#".repeat(level)).append(" ").append(paragraph.text).append("\n\n")
                }
                paragraph.isBullet() -> markdown.append("- ").append(paragraph.text).append("\n")
                paragraph.isNumbered() -> markdown.append("1. ").append(paragraph.text).append("\n")
                else -> markdown.append(paragraph.text).append("\n\n")
            }
        }

        for (table in document.tables) {
            markdown.append("\n")
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    markdown.append("| ").append(cell.text).append(" ")
                }
                markdown.append("|\n")
            }
            markdown.append("\n")
        }

        document.close()
        return markdown.toString()
    }

    private fun processDocToMarkdown(document: HWPFDocument): String {
        val markdown = StringBuilder()
        val range = document.range

        for (i in 0 until range.numParagraphs()) {
            val paragraph = range.getParagraph(i)
            val text = paragraph.text().trim()

            when {
                paragraph.isHeading() -> {
                    val level = paragraph.getLevel()
                    markdown.append("#".repeat(level)).append(" ").append(text).append("\n\n")
                }
                paragraph.isBullet() -> markdown.append("- ").append(text).append("\n")
                paragraph.isNumbered() -> markdown.append("1. ").append(text).append("\n")
                else -> markdown.append(text).append("\n\n")
            }
        }

        document.close()
        return markdown.toString()
    }

    private fun XWPFParagraph.isBullet(): Boolean {
        return this.numFmt?.lowercase() == "bullet"
    }

    private fun XWPFParagraph.isNumbered(): Boolean {
        return this.numFmt?.lowercase() == "decimal"
    }

    private fun Paragraph.isHeading(): Boolean {
        return this.justification == 1
    }

    private fun Paragraph.getLevel(): Int {
        return if (this.isHeading()) 2 else 1
    }

    private fun Paragraph.isBullet(): Boolean {
        return this.ilvl.toInt() == 0
    }

    private fun Paragraph.isNumbered(): Boolean {
        return this.ilvl > 0.toShort()
    }
}
