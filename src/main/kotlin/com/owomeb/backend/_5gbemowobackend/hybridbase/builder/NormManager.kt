package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import org.springframework.stereotype.Component
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipInputStream

@Component
class NormManager {

    fun isNormaDownloaded(normPath: String): Boolean {
        val file = File(normPath)
        if (!file.exists()) return false

        return try {
            val lines = file.readLines()
            val content = file.readText()

            lines.size >= 5 || content.length >= 200
        } catch (e: Exception) {
            println("Norma istnieje ale jest za krótka")
            false
        }
    }

    fun downloadAndExtractNorm(normUrl: String, zipPath: String, docPath: String): Boolean {
        return try {
            val zipFile = File(zipPath)
            zipFile.parentFile?.mkdirs()

            val url = URL(normUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                println("Błąd pobierania normy: ${connection.responseCode}")
                return false
            }

            Files.copy(connection.inputStream, Paths.get(zipFile.toURI()))

            extractZip(zipFile, docPath)
        } catch (e: Exception) {
            println("Błąd: ${e.message}")
            false
        }
    }

    private fun extractZip(zipFile: File, docPath: String): Boolean {
        println("ZipPath: ${zipFile.absolutePath}")
        println("DocPath (bez rozszerzenia): $docPath")

        return try {
            ZipInputStream(FileInputStream(zipFile)).use { zipInputStream ->
                var zipEntry = zipInputStream.nextEntry
                var docFile: File? = null

                while (zipEntry != null) {
                    if (zipEntry.name.endsWith(".doc", ignoreCase = true) || zipEntry.name.endsWith(".docx", ignoreCase = true)) {
                        //val extension = if (zipEntry.name.endsWith(".docx", ignoreCase = true)) ".docx" else ".doc"
                        docFile = File(docPath)
                        println("ZIP jest rozpakowywany jako jako: ${docFile.name}")
                        FileOutputStream(docFile).use { outputStream ->
                            zipInputStream.copyTo(outputStream)
                        }
                    }
                    zipEntry = zipInputStream.nextEntry
                }

                zipFile.delete()
                docFile?.exists() == true
            }
        } catch (e: Exception) {
            println("Błąd rozpakowywania ZIP: ${e.message}")
            false
        }
    }

}
