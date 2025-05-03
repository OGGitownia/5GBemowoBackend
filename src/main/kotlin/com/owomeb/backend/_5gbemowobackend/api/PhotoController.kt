package com.owomeb.backend._5gbemowobackend.api

import org.freehep.graphicsio.emf.EMFInputStream
import org.freehep.graphicsio.emf.EMFRenderer
import org.springframework.http.*
import org.springframework.web.bind.annotation.*
import java.awt.*
import java.awt.image.BufferedImage
import java.io.*
import java.nio.file.*
import javax.imageio.ImageIO

@RestController
@RequestMapping("/api/photos")
class PhotoController {

    private val basePath = Paths.get("src/main/resources/data")

    @GetMapping("/{folder}/{filename}")
    fun getConvertedPhoto(
        @PathVariable folder: Int,
        @PathVariable filename: String
    ): ResponseEntity<*> {
        val photoDir = basePath.resolve(folder.toString()).resolve("photos")

        var targetFile = photoDir.resolve(filename)
        var extension = com.google.common.io.Files.getFileExtension(filename).lowercase()

        if (!Files.exists(targetFile) && extension == "png") {
            val emfFilename = filename.replace(".png", ".emf")
            val emfFilePath = photoDir.resolve(emfFilename)
            if (Files.exists(emfFilePath)) {
                targetFile = emfFilePath
                extension = "emf"
            }
        }


        if (!Files.exists(targetFile)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Plik nie istnieje: $filename")
        }

        val output = ByteArrayOutputStream()

        return try {
            val image: BufferedImage? = when (extension) {
                "png" -> ImageIO.read(targetFile.toFile())
                "emf" -> convertEmfToImage(targetFile.toFile())
                else -> return ResponseEntity
                    .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Nieobsługiwany format: $extension (obsługiwane: .png, .emf)")
            }

            if (image == null) {
                return ResponseEntity
                    .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .body("Nie udało się odczytać obrazu.")
            }

            ImageIO.write(image, "png", output)
            val bytes = output.toByteArray()

            ResponseEntity
                .ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(bytes)

        } catch (e: Exception) {
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Błąd: ${e.message}")
        }
    }


    private fun convertEmfToImage(file: File): BufferedImage {
        FileInputStream(file).use { fis ->
            val emfStream = EMFInputStream(fis, EMFInputStream.DEFAULT_VERSION)
            val renderer = EMFRenderer(emfStream)
            val size = emfStream.readHeader().bounds.size
            val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB)
            val g2 = image.createGraphics()
            renderer.paint(g2)
            g2.dispose()
            return image
        }
    }
}
