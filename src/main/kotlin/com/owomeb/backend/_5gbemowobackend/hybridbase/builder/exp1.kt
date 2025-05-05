package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.*
import java.io.*

class DocxElementExtractor {

    fun extractElements(inputPath: String, outputDir: String) {
        val inputFile = File(inputPath)
        val inputStream = FileInputStream(inputFile)
        val originalDoc = XWPFDocument(inputStream)

        extractTables(originalDoc, File(outputDir, "only_tables.docx"))
        extractImages(originalDoc, File(outputDir, "only_images.docx"))
        extractListings(originalDoc, File(outputDir, "only_listings.docx"))
        extractCharts(originalDoc, File(outputDir, "only_charts.docx"))

        originalDoc.close()
        inputStream.close()

        println("Ekstrakcja zakończona.")
    }

    private fun extractTables(doc: XWPFDocument, outputFile: File) {
        val newDoc = XWPFDocument()
        for (table in doc.tables) {
            val newTable = newDoc.createTable()
            newTable.removeRow(0)
            for (row in table.rows) {
                val newRow = newTable.createRow()
                for ((i, cell) in row.tableCells.withIndex()) {
                    if (i < newRow.tableCells.size) {
                        newRow.getCell(i).text = cell.text
                    } else {
                        newRow.addNewTableCell().text = cell.text
                    }
                }
            }
        }
        saveDoc(newDoc, outputFile)
    }

    private fun extractImages(doc: XWPFDocument, outputFile: File) {
        val newDoc = XWPFDocument()
        for (paragraph in doc.paragraphs) {
            val newParagraph = newDoc.createParagraph()
            for (run in paragraph.runs) {
                val pictures = run.embeddedPictures
                for (picture in pictures) {
                    val picRun = newParagraph.createRun()
                    picRun.addPicture(
                        ByteArrayInputStream(picture.pictureData.data),
                        getPictureType(picture.pictureData.pictureType),
                        picture.pictureData.fileName,
                        400_000, 300_000
                    )
                }
            }
        }
        saveDoc(newDoc, outputFile)
    }

    private fun extractListings(doc: XWPFDocument, outputFile: File) {
        val newDoc = XWPFDocument()
        for (paragraph in doc.paragraphs) {
            val isListing = paragraph.runs.any {
                it.fontFamily?.lowercase()?.contains("courier") == true || it.isBold && it.text().contains(";")
            }
            if (isListing) {
                val p = newDoc.createParagraph()
                val r = p.createRun()
                r.setText(paragraph.text)
            }
        }
        saveDoc(newDoc, outputFile)
    }

    private fun extractCharts(doc: XWPFDocument, outputFile: File) {
        val newDoc = XWPFDocument()
        val paragraph = newDoc.createParagraph()
        val run = paragraph.createRun()
        val charts = doc.allEmbeddedParts.filter { it is XWPFChart }

        if (charts.isEmpty()) {
            run.setText("Nie znaleziono wykresów.")
        } else {
            for ((index, chartPart) in charts.withIndex()) {
                run.addBreak()
                run.setText("Znaleziono wykres #$index w pliku: ${chartPart.partName}")
            }
        }
        saveDoc(newDoc, outputFile)
    }

    private fun saveDoc(doc: XWPFDocument, file: File) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { doc.write(it) }
        doc.close()
        println("Zapisano: ${file.path}")
    }

    private fun getPictureType(pictureType: Int): Int {
        return when (pictureType) {
            Document.PICTURE_TYPE_PNG -> Document.PICTURE_TYPE_PNG
            Document.PICTURE_TYPE_JPEG -> Document.PICTURE_TYPE_JPEG
            Document.PICTURE_TYPE_GIF -> Document.PICTURE_TYPE_GIF
            else -> Document.PICTURE_TYPE_PNG
        }
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            val inputFile = "src/main/resources/data/11/norm.docx"
            val outputDir = "src/main/resources/extracted/"

            DocxElementExtractor().extractElements(inputFile, outputDir)
        }
    }
}
