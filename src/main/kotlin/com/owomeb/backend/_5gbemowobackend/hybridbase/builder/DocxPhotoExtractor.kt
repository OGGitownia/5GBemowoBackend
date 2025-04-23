package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.apache.poi.xwpf.usermodel.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import org.apache.poi.xwpf.usermodel.XWPFRun
import org.apache.poi.xwpf.usermodel.XWPFPictureData
import org.openxmlformats.schemas.drawingml.x2006.picture.CTPicture
import org.apache.xmlbeans.XmlCursor
import org.apache.xmlbeans.XmlObject


class DocxPhotoExtractor(
    private val inputDocxPath: String,
    private val outputDirPath: String,
    private val outputDocxPath: String
) {

    private var pictureIndex = 1
    private val outputDir = File(outputDirPath, "photos")

    fun runPhotoAction() {
        outputDir.mkdirs()

        val document = XWPFDocument(FileInputStream(File(inputDocxPath)))

        processParagraphs(document.paragraphs)
        processTables(document.tables)

        document.headerList.forEach { header ->
            processParagraphs(header.paragraphs)
            processTables(header.tables)
        }

        document.footerList.forEach { footer ->
            processParagraphs(footer.paragraphs)
            processTables(footer.tables)
        }

        FileOutputStream(File(outputDocxPath)).use { out ->
            document.write(out)
        }

        println("Zdjęcia zapisane do: ${outputDir.absolutePath}")
        println("Zmodyfikowany dokument zapisany jako: $outputDocxPath")
    }

    private fun processParagraphs(paragraphs: List<XWPFParagraph>) {
        paragraphs.forEach { para ->
            para.runs?.forEach { run ->
                extractAndReplacePictures(run)
            }
        }
    }

    private fun processTables(tables: List<XWPFTable>) {
        tables.forEach { table ->
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    processParagraphs(cell.paragraphs)
                    processTables(cell.tables)
                }
            }
        }
    }


    private fun extractAndReplacePictures(run: XWPFRun) {
        val pictures = run.embeddedPictures
        if (pictures.isNotEmpty()) {
            pictures.forEach { picture ->
                savePictureAndReplaceRunText(run, picture.pictureData)
            }
        }

        val drawings = run.ctr.drawingArray
        drawings.forEach { drawing ->
            val inlineList = drawing.inlineList
            inlineList.forEach { inline ->
                val graphicData = inline.graphic.graphicData

                // Szukamy obiektu CTPicture w środku XML-a
                val xmlCursor: XmlCursor = graphicData.newCursor()
                xmlCursor.selectPath("./*")

                while (xmlCursor.toNextSelection()) {
                    val xmlObj: XmlObject = xmlCursor.`object`
                    if(xmlObj is CTPicture) {
                        val blip = xmlObj.blipFill.blip
                        val embedId = blip.embed
                        val pictureData: XWPFPictureData? = run.document.getPictureDataByID(embedId)
                        if (pictureData != null) {
                            savePictureAndReplaceRunText(run, pictureData)
                        }
                    }
                }

                xmlCursor.dispose()
            }
        }
    }



    private fun savePictureAndReplaceRunText(run: XWPFRun, pictureData: XWPFPictureData) {
        val extension = pictureData.suggestFileExtension()
       // if (extension == "emf") return  // pomijaj emf

        val fileName = "photo_${pictureIndex++}.$extension"
        val filePath = File(outputDir, fileName)

        FileOutputStream(filePath).use { fos ->
            fos.write(pictureData.data)
        }

        run.setText("{zdjęcie zapisane jako: $fileName}", 0)
    }
}
