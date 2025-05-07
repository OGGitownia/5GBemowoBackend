package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.PictureType
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.poi.hwpf.usermodel.Paragraph
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.springframework.stereotype.Component


@Component
class NewMarkDowns {

    fun TEST(inputPath: String, outputPath: String) {
        println("=== TEST START ===")

        val norm36 = "$inputPath/36/norm"
        val norm26 = "$inputPath/26/norm"

        val out36 = "$outputPath/36"
        val out26 = "$outputPath/26"

        val success36 = convertDocumentToMarkdown(norm36, out36)
        println("36/norm → ${if (success36) " OK" else "Błąd"}")

        val success26 = convertDocumentToMarkdown(norm26, out26)
        println("26/norm → ${if (success26) "OK" else "Błąd"}")

        println("=== TEST END ===")
    }

    fun isMarkdownExists(markdownPath: String): Boolean {
        return File(markdownPath).exists()
    }

    fun convertDocumentToMarkdown(normPath: String, markdownPath: String): Boolean {
        println("convertDocumentToMarkdown szuka: $normPath")
        return try {
            val docxFile = File("$normPath.docx")
            val docFile = File("$normPath.doc")

            when {
                docxFile.exists() -> {
                    println("Znaleziono DOCX")
                    convertDocxToMarkdown(
                        docxFile = docxFile,
                        outputPath = markdownPath
                    )
                }
                docFile.exists() -> {
                    println("Znaleziono DOC")
                    convertDocToMarkdown(docFile, markdownPath)
                }
                else -> {
                    println("Error i chuj")
                    return false
                }
            }


            //val finalMarkdownFile = File(markdownPath, "$baseName.md")
            //rawMarkdownFile.copyTo(finalMarkdownFile, overwrite = true)
            println("Markdown utworzony: $markdownPath")

            true
        } catch (e: Exception) {
            println("Error: ${e.message}")
            false
        }
    }


    fun convertDocToMarkdown(docFile: File, outputPath: String): File {
        val doc = HWPFDocument(FileInputStream(docFile))
        val markdown = StringBuilder()
        val range = doc.range
        var numberedListCounter = 1

        // Katalog docelowy na markdown (.md)
        val markdownFile = File(outputPath)
        val markdownDir = markdownFile.parentFile
        markdownDir.mkdirs()

        // Katalog na obrazki obok pliku markdown
        val imageDirName = markdownFile.nameWithoutExtension + "_images"
        val imageDir = File(markdownDir, imageDirName)
        imageDir.mkdirs()

        for (i in 0 until range.numParagraphs()) {
            val paragraph: Paragraph = range.getParagraph(i)
            var text = paragraph.text()

            // Usuwanie niechcianych znaków
            text = removeUnwantedCharacters(text)

            // Usuwanie śmieci i spisów treści
            if (removeTableOfContents(text) || cleanGarbageLines(text)) continue

            // Formatowanie spacji i list
            text = formatSpacesAndLists(text)

            if (text.isBlank()) continue

            // Formatowanie nagłówków i list
            val isFormalHeading = text.matches(Regex("""^\d+(\.\d+)*\s+.+"""))
            val isAllCapsHeading = text.matches(Regex("^[A-Z0-9 ,.:\\-]{5,}$"))
            val isHeading = isFormalHeading || isAllCapsHeading
            val isBullet = text.startsWith("-") || text.startsWith("•")
            val isNumbered = text.matches(Regex("^\\d+[.>]\\s+.*"))

            when {
                isHeading -> {
                    markdown.append("### ").append(text.trim()).append("\n\n")
                    numberedListCounter = 1
                }
                isBullet -> {
                    markdown.append("- ").append(text.removePrefix("- ").removePrefix("• ")).append("\n\n")
                }
                isNumbered -> {
                    markdown.append("1. ").append(text.substringAfter(" ")).append("\n\n")
                }
                else -> {
                    markdown.append(text.trim()).append("\n\n")
                }
            }
        }

        // Przetwarzanie grafik i dodanie znaczników do markdowna
        val imageNotices = extractAndReplaceImages(doc, imageDir)
        imageNotices.forEach { markdown.append(it).append("\n\n") }

        doc.close()

        // Zapis markdown
        markdownFile.writeText(markdown.toString())
        println("Markdown zapisany pod: ${markdownFile.absolutePath}")
        println("Obrazy w katalogu: ${imageDir.absolutePath}")
        return markdownFile
    }


    fun removeUnwantedCharacters(text: String): String {
        return text
            .replace("\u0007", "")
            .replace("\u000b", "")
            .replace("\u0013", "")
            .replace("\u0014", "")
            .replace("\u0015", "")
            .replace(Regex("[\\r\\n]+"), " ")
            .trim()
    }

    fun removeTableOfContents(text: String): Boolean {
        return text.contains("TOC", ignoreCase = true) || text.contains("PAGEREF", ignoreCase = true)
    }

    fun cleanGarbageLines(text: String): Boolean {
        return text.contains("HYPERLINK", true) ||
                text.contains("EMBED", true) ||
                text.matches(Regex("^\\s*$")) ||
                text.length < 3
    }

    fun formatSpacesAndLists(text: String): String {
        return text
            .replace(Regex("""\s+"""), " ")
            .replace(Regex("""(\d+)>"""), "$1.")
            .trim()
    }

    fun extractAndReplaceImages(doc: HWPFDocument, outputDir: File): List<String> {
        val notices = mutableListOf<String>()

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
            val imageFile = File(outputDir, imageName)

            try {
                FileOutputStream(imageFile).use { out -> out.write(picture.content) }
                notices.add("**{uwaga niemiecki obraz:$imageName:}**")
            } catch (e: Exception) {
                println("❌ Błąd przy zapisie obrazka: ${e.message}")
            }
        }

        return notices
    }



    fun convertDocxToMarkdown(docxFile: File, outputPath: String): File {
        val docx = XWPFDocument(FileInputStream(docxFile))
        val markdown = StringBuilder()
        var numberedListCounter = 1

        // Plik docelowy markdown
        val markdownFile = File(outputPath)
        val markdownDir = markdownFile.parentFile
        markdownDir.mkdirs()

        // Katalog na obrazy obok pliku .md
        val imageDirName = markdownFile.nameWithoutExtension + "_images"
        val imageDir = File(markdownDir, imageDirName)
        imageDir.mkdirs()

        for (paragraph in docx.paragraphs) {
            var text = paragraph.text ?: continue
            text = removeUnwantedCharacters(text)

            if (removeTableOfContents(text) || cleanGarbageLines(text)) continue

            text = formatSpacesAndLists(text)

            if (text.isBlank()) continue

            val isFormalHeading = text.matches(Regex("""^\d+(\.\d+)*\s+.+"""))
            val isAllCapsHeading = text.matches(Regex("^[A-Z0-9 ,.:\\-]{5,}$"))
            val isHeading = isFormalHeading || isAllCapsHeading
            val isBullet = text.startsWith("-") || text.startsWith("•")
            val isNumbered = text.matches(Regex("^\\d+[.>]\\s+.*"))

            when {
                isHeading -> {
                    markdown.append("### ").append(text.trim()).append("\n\n")
                    numberedListCounter = 1
                }
                isBullet -> {
                    markdown.append("- ").append(text.removePrefix("- ").removePrefix("• ")).append("\n\n")
                }
                isNumbered -> {
                    markdown.append("1. ").append(text.substringAfter(" ")).append("\n\n")
                }
                else -> {
                    markdown.append(text.trim()).append("\n\n")
                }
            }
        }

        // Przetwarzanie grafik z docx
        val imageNotices = extractAndReplaceImagesFromDocx(docx, imageDir)
        imageNotices.forEach { markdown.append(it).append("\n\n") }

        docx.close()

        // Zapis markdown
        markdownFile.writeText(markdown.toString())
        println("Markdown zapisany pod: ${markdownFile.absolutePath}")
        println("Obrazy zapisane w: ${imageDir.absolutePath}")
        return markdownFile
    }


    fun extractAndReplaceImagesFromDocx(docx: XWPFDocument, outputDir: File): List<String> {
        val notices = mutableListOf<String>()

        val pictures = docx.allPictures
        for ((index, pictureData) in pictures.withIndex()) {
            val ext = pictureData.suggestFileExtension() ?: "bin"
            val imageName = "image$index.$ext"
            val imageFile = File(outputDir, imageName)

            try {
                FileOutputStream(imageFile).use { it.write(pictureData.data) }
                notices.add("**{uwaga niemiecki obraz:$imageName:}**")
            } catch (e: Exception) {
                println("❌ Błąd przy zapisie obrazka (docx): ${e.message}")
            }
        }

        return notices
    }




    fun analyseMarkdown(markdownFile: File, outputReportPath: String) {
        val lines = markdownFile.readLines()
        val report = StringBuilder()
        var lineNumber = 1

        report.append("Markdown Analysis Report\n")
        report.append("File: ${markdownFile.name}\n")
        report.append("====================================\n\n")

        for (line in lines) {
            val trimmed = line.trim()

            // Artefakty Worda
            if (trimmed.contains("EMBED") || trimmed.contains("PAGEREF") || trimmed.contains("TOC")) {
                report.append("[$lineNumber] ⚠️ Possible Word artifact detected: '$trimmed'\n")
            }

            if (trimmed.contains("\u0013") || trimmed.contains("\u0014") || trimmed.contains("\u0015")) {
                report.append("[$lineNumber] ⚠️ Binary/hidden Word tag found (e.g. \\u0013)\n")
            }

            // Sprawdzenie nagłówków bez #
            if (Regex("""^\d+(\.\d+)*\s+[A-Z]""").matches(trimmed)) {
                report.append("[$lineNumber] ⚠️ Header-like line found but missing '#': '$trimmed'\n")
            }

            // Brak pustej linii po nagłówku
            if (trimmed.startsWith("#") && lineNumber < lines.size - 1) {
                val next = lines[lineNumber].trim()
                if (next.isNotBlank() && !next.startsWith("-") && !next.startsWith("1.")) {
                    report.append("[$lineNumber] ⚠️ Header not followed by a blank line\n")
                }
            }

            // Błędy w listach numerowanych
            if (trimmed.matches(Regex("""\d+\>\s+.*"""))) {
                report.append("[$lineNumber] ⚠️ Unconventional numbered list using '1>' instead of '1.'\n")
            }

            // Nieprawidłowe osadzenie obrazu
            if (trimmed.contains("EMBED Picture") || trimmed.contains("Word.Picture")) {
                report.append("[$lineNumber] ⚠️ Image embed placeholder found — not valid Markdown: '$trimmed'\n")
            }

            lineNumber++
        }

        if (report.isEmpty()) {
            report.append("No issues found — Markdown looks clean!\n")
        }

        val outputFile = File(outputReportPath)
        outputFile.writeText(report.toString())
    }
}
