package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import java.io.FileInputStream
import java.io.FileOutputStream

fun main() {
    val inputPath = "src/main/resources/data/59/extractedNorm.docx"
    val outputPath = "src/main/resources/data/59/normWithoutTableOfContent.docx"

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

    FileOutputStream(outputPath).use { out ->
        doc.write(out)
    }

    println("✅ Zapisano dokument do: $outputPath")
}