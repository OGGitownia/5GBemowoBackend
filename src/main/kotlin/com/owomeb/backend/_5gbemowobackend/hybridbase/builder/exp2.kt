package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class CleanedNormGenerator {

    fun cleanNorm(inputPath: String, outputPath: String) {
        val inputStream = FileInputStream(inputPath)
        val document = XWPFDocument(inputStream)

        val elements = document.bodyElements

        // 1. Usuń tabele i wstaw "TABLE"
        for (i in elements.size - 1 downTo 0) {
            val element = elements[i]
            if (element.elementType.name == "TABLE") {
                document.removeBodyElement(i)
                val paragraph = document.createParagraph()
                paragraph.createRun().setText("TABLE")
            }
        }

        // 2. Zastąp nagłówki napisem "HEADER"
        for (paragraph in document.paragraphs) {
            if (paragraph.style?.lowercase()?.startsWith("heading") == true) {
                removeAllRuns(paragraph)
                paragraph.createRun().setText("HEADER")
                paragraph.style = null
            }
        }

        // 3. Zastąp obrazki napisem "PHOTO"
        for (paragraph in document.paragraphs) {
            for (run in paragraph.runs) {
                if (run.embeddedPictures.isNotEmpty()) {
                    removeAllRuns(paragraph)
                    paragraph.createRun().setText("PHOTO")
                    break
                }
            }
        }

        // 4. Usuń spis treści (długą listę numerowaną na początku)
        val paragraphs = document.paragraphs
        if (document.numbering != null) {
            val tocEndIndex = detectTableOfContentsEndIndex(paragraphs)
            if (tocEndIndex > 3) {
                for (i in tocEndIndex - 1 downTo 0) {
                    val index = document.paragraphs.indexOf(paragraphs[i])
                    if (index >= 0) {
                        document.removeBodyElement(index)
                    }
                }
                val p = document.createParagraph()
                p.createRun().setText("TOC_REMOVED")
            }
        }

        // Zapisz dokument
        val outputFile = File(outputPath)
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { document.write(it) }

        document.close()
        inputStream.close()

        println("Wyczyszczona norma zapisana do: $outputPath")
    }

    private fun removeAllRuns(paragraph: org.apache.poi.xwpf.usermodel.XWPFParagraph) {
        while (paragraph.runs.size > 0) {
            paragraph.removeRun(0)
        }
    }

    private fun detectTableOfContentsEndIndex(paragraphs: List<org.apache.poi.xwpf.usermodel.XWPFParagraph>): Int {
        var count = 0
        for ((i, p) in paragraphs.withIndex()) {
            if (p.numID != null) {
                count++
                if (count >= 5) return i + 1
            } else {
                break
            }
        }
        return 0
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val inputFile = "src/main/resources/data/11/norm.docx"
            val outputFile = "src/main/resources/cleaned/cleaned_norm3.docx"
            CleanedNormGenerator().cleanNorm(inputFile, outputFile)
        }
    }
}
