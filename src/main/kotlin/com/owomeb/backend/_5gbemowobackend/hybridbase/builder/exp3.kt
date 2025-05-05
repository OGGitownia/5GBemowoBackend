package com.owomeb.backend._5gbemowobackend.hybridbase.builder



import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import org.apache.poi.xwpf.usermodel.XWPFSDT

class DocxToMarkdownConverter {

    fun convertToMarkdown(inputPath: String, outputPath: String) {
        val document = XWPFDocument(FileInputStream(inputPath))
        val sb = StringBuilder()

        // Bufor do detekcji spisu treści
        val lines = mutableListOf<String>()
        val elements = document.bodyElements



                for (element in elements) {
                    when (element.elementType) {
                        BodyElementType.PARAGRAPH -> {
                            val para = element as XWPFParagraph
                            if (para.style?.lowercase()?.startsWith("heading") == true) continue
                            val lineText = para.text
                            if (lineText.isNotBlank()) lines.add(lineText)
                        }

                        BodyElementType.TABLE -> {
                            lines.add("TABLE")
                        }

                        BodyElementType.CONTENTCONTROL -> {
                            if (element is XWPFSDT) {
                                val text = element.content.text
                                if (text.isNotBlank()) {
                                    lines.addAll(text.lines())
                                }
                            }
                        }
                    }
                }



        // Usuń spis treści
        val contentLines = removeTableOfContents(lines)

        // Zamień zdjęcia i zapisz
        for (line in contentLines) {
            if (line.contains(Regex(".*\\.jpg|.*\\.png|.*\\.jpeg"))) {
                sb.appendLine("PHOTO")
            } else {
                sb.appendLine(line)
            }
        }

        File(outputPath).parentFile?.mkdirs()
        FileWriter(outputPath).use { it.write(sb.toString()) }

        println("Markdown zapisany do: $outputPath")
    }

    private fun removeTableOfContents(lines: List<String>): List<String> {
        val result = mutableListOf<String>()
        var insideTOC = false
        var lookahead = 5

        for ((i, line) in lines.withIndex()) {
            val dotRatio = line.count { it == '.' }.toDouble() / line.length
            if (dotRatio > 0.10) {
                insideTOC = true
                lookahead = 5
                continue
            }
            if (insideTOC && lookahead > 0) {
                lookahead--
                continue
            }
            result.add(line)
        }

        if (!insideTOC) {
            throw Exception("Nie znaleziono spisu treści.")
        }

        return result
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val inputPath = "src/main/resources/data/52/norm.docx"
            val outputPath = "src/main/resources/cleaned/cleaned_norm7.md"
            DocxToMarkdownConverter().convertToMarkdown(inputPath, outputPath)
        }
    }
}
