package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.Paragraph
import org.apache.poi.hwpf.usermodel.PictureType
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

    fun convertDocumentToMarkdown(docPath: String, markdownPath: String, pureMarkdownPath: String): Boolean {
        println("convertDocumentToMarkdown szuka: $docPath")

        return try {
            val docxFile = File("$docPath.docx")
            val docFile = File("$docPath.doc")
            val rawMarkdownFile: File

            rawMarkdownFile = when {
                docxFile.exists() -> {
                    println("Znaleziono DOCX")
                    val docx = XWPFDocument(FileInputStream(docxFile))
                    processDocxToMarkdown(docx, docxFile)
                }

                docFile.exists() -> {
                    println("Znaleziono DOC")
                    convertDocToMarkdown2(docFile)
                }

                else -> {
                    println("Błąd: ani DOCX ani DOC nie istnieje dla: $docPath")
                    return false
                }
            }

            val finalMarkdown = File(markdownPath)
            rawMarkdownFile.copyTo(finalMarkdown, overwrite = true)
            println("Markdown zapisany: ${finalMarkdown.absolutePath}")

            cleanMarkdown(markdownPath, pureMarkdownPath)
            println("Markdown oczyszczony: $pureMarkdownPath")

            true
        } catch (e: Exception) {
            println("Error: ${e.message}")
            false
        }
    }





    fun cleanMarkdown(markdownPath: String, clearMarkdownPath: String) {
        val inputFile = File(markdownPath)
        val outputFile = File(clearMarkdownPath)

        if (!inputFile.exists()) {
            println("Plik nie istnieje: $markdownPath")
            return
        }

        val cleanedLines = mutableListOf<String>()
        var isInTableOfContents = false
        var isBeforeMainContent = true
        val removedLines = mutableListOf<String>()

        inputFile.forEachLine { line ->
            val trimmedLine = line.trim()

            // USUWANIE SPISU TREŚCI DZIAŁAŁO DLA LTE W DOC
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
                // koniec gdy patern treść dokumentu, kończymy czyszczenie
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



    private fun processDocxToMarkdown(document: XWPFDocument, docxFile: File): File {
        println("Norm was docX")
        val markdown = StringBuilder()
        var numberedListCounter = 1

        fun cleanText(text: String): String {
            return text
                .replace("\u0007", "")  // BEL
                .replace("\u0013", "")  // hidden start
                .replace("\u0014", "")  // hidden separator
                .replace("\u0015", "")  // hidden end
                .trim()
        }

        for (paragraph in document.paragraphs) {
            val rawText = paragraph.text ?: continue
            val text = cleanText2(rawText)

            if (text.isBlank()) continue
            if (text.contains("PAGEREF", true) || text.contains("TOC", true) ||
                text.contains("HYPERLINK", true) || text.contains("EMBED", true)) continue

            val isFormalHeading = text.matches(Regex("""^\d+(\.\d+)*\s+.+"""))
            val isAllCapsHeading = text.matches(Regex("^[A-Z0-9 ,.:\\-]{5,}$"))
            val isHeading = isFormalHeading || isAllCapsHeading

            val isBullet = paragraph.numFmt == "bullet"
            val isNumbered = paragraph.numFmt == "decimal"

            when {
                isHeading -> {
                    markdown.append("### ").append(text).append("\n\n")
                    numberedListCounter = 1
                }
                isBullet -> markdown.append("- ").append(text).append("\n")
                isNumbered -> markdown.append("${numberedListCounter++}. ").append(text).append("\n")
                else -> markdown.append(text).append("\n\n")
            }
        }

        // Tables
        for (table in document.tables) {
            markdown.append("\n")
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    markdown.append("| ").append(cleanText(cell.text)).append(" ")
                }
                markdown.append("|\n")
            }
            markdown.append("\n")
        }

        // Images
        val imageDir = File(docxFile.parentFile, "images")
        if (!imageDir.exists()) imageDir.mkdirs()

        val pictures = document.allPictures
        pictures.forEachIndexed { index, picture ->
            val ext = picture.suggestFileExtension() ?: "img"
            val imageName = "image$index.$ext"
            val imageFile = File(imageDir, imageName)
            FileOutputStream(imageFile).use { it.write(picture.data) }

            markdown.append("![Image $index](images/$imageName)\n\n")
        }

        document.close()

        // Output file
        val outputFile = File.createTempFile("generated_docx_", ".md")
        outputFile.writeText(markdown.toString())
        return outputFile
    }

    // artefakt word doc i docx
    private fun cleanText2(text: String): String {
        return text
            .replace("\u0007", "") // BEL
            .replace("\u0013", "") // pole zaczynające
            .replace("\u0014", "") // separator
            .replace("\u0015", "") // pole końcowe
            .replace("\r", "")
            .trim()
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

    fun convertDocToMarkdown2(docFile: File): File {
        println("Norm was doc")
        val doc = HWPFDocument(FileInputStream(docFile))
        val markdown = StringBuilder()
        val range = doc.range
        var numberedListCounter = 1

        for (i in 0 until range.numParagraphs()) {
            val paragraph: Paragraph = range.getParagraph(i)
            val rawText = paragraph.text()
            val text = cleanText(rawText)

            if (text.isBlank()) continue
            if (text.contains("PAGEREF", true) || text.contains("TOC", true) ||
                text.contains("HYPERLINK", true) || text.contains("EMBED", true)) continue

            val isFormalHeading = text.matches(Regex("""^\d+(\.\d+)*\s+.+"""))
            val isAllCapsHeading = text.matches(Regex("^[A-Z0-9 ,.:\\-]{5,}$"))
            val isHeading = isFormalHeading || isAllCapsHeading

            val isBullet = text.startsWith("-") || text.startsWith("•")
            val isNumbered = text.matches(Regex("^\\d+[.>]\\s+.*"))

            when {
                isHeading -> {
                    markdown.append("### ").append(text).append("\n\n")
                    numberedListCounter = 1
                }
                isBullet -> {
                    markdown.append("- ").append(text.removePrefix("- ").removePrefix("• ")).append("\n")
                }
                isNumbered -> {
                    markdown.append("${numberedListCounter++}. ").append(text.substringAfter(" ")).append("\n")
                }
                else -> {
                    markdown.append(text).append("\n\n")
                }
            }
        }

        // !!!!!!
        val imageDir = File(docFile.parentFile, "images")
        if (!imageDir.exists()) imageDir.mkdirs()

        doc.picturesTable.allPictures.forEachIndexed { index, picture ->
            val ext = when (picture.suggestPictureType()) {
                PictureType.PNG -> "png"
                PictureType.JPEG -> "jpg"
                PictureType.GIF -> "gif"
                PictureType.BMP -> "bmp"
                PictureType.WMF -> "wmf"
                else -> "bin"
            }

            val imageName = "image$index.$ext"
            val imageFile = File(imageDir, imageName)
            FileOutputStream(imageFile).use { out -> out.write(picture.content) }

            markdown.append("![Image $index](images/$imageName)\n\n")
        }

        doc.close()

        val outputFile = File.createTempFile("generated_", ".md")
        outputFile.writeText(markdown.toString())
        return outputFile
    }

    fun cleanText(text: String): String {
        return text
            .replace("\u0007", "")
            .replace("\u000b", "")
            .replace("\u0013", "")
            .replace("\u0014", "")
            .replace("\u0015", "")
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""(\d+)>"""), "$1.")
            .trim()
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
