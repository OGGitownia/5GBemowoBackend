package com.owomeb.backend._5gbemowobackend.hybridbase.builder
import org.apache.poi.xwpf.usermodel.XWPFDocument
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

class DocxPhotoExtractor(
    private val inputDocxPath: String,
    private val outputDirPath: String,
    private val outputDocxPath: String
) {

    fun runPhotoAction() {
        val inputFile = File(inputDocxPath)
        val outputDir = File(outputDirPath, "photos")
        outputDir.mkdirs()


        val document = XWPFDocument(FileInputStream(inputFile))

        var pictureIndex = 1

        for (para in document.paragraphs) {
            val runs = para.runs
            if (runs != null) {
                for (run in runs) {
                    val embeddedPictures = run.embeddedPictures
                    if (embeddedPictures != null && embeddedPictures.isNotEmpty()) {
                        for (picture in embeddedPictures) {
                            val pictureData = picture.pictureData
                            val extension = pictureData.suggestFileExtension()
                            val fileName = "photo_$pictureIndex.$extension"
                            val filePath = File(outputDir, fileName)

                            // Zapisz zdjęcie
                            FileOutputStream(filePath).use { fos ->
                                fos.write(pictureData.data)
                            }

                            // Zamień obrazek na tekst
                            run.setText("{zdjęcie zapisane jako: $fileName}", 0)
                            pictureIndex++
                        }
                    }
                }
            }
        }

        // Zapisz zmodyfikowany dokument do outputDocxPath
        val outputDocxFile = File(outputDocxPath)
        FileOutputStream(outputDocxFile).use { out ->
            document.write(out)
        }

        println("Zdjęcia zapisane do: ${outputDir.absolutePath}")
        println("Zmodyfikowany dokument zapisany jako: ${outputDocxFile.absolutePath}")
    }
}

