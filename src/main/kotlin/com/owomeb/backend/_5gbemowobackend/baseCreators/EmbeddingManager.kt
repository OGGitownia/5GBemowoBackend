package com.owomeb.backend._5gbemowobackend.baseCreators

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@Service
class EmbeddingManager @Autowired constructor(
    private val flaskServerService: FlaskServerService
) {


    fun isEmbeddedJsonExist(embJsonPath: String): Boolean {
        val file = File(embJsonPath)
        if (!file.exists()) return false

        return try {
            val content = file.readText().trim()
            content.isNotEmpty() && content.length >= 200
        } catch (e: Exception) {
            println("Plik jest, ale jest za krótki (ten plik w formacie Json)")
            false
        }
    }

    fun countFragmentsWithoutEmbedding(embeddedJsonPath: String): Int {
        val file = File(embeddedJsonPath)
        if (!file.exists() || file.length() <= 200) {
            println(" Plik nie istnieje lub jest pusty.")
            return -1
        }

        val objectMapper = jacksonObjectMapper()
        val jsonData: JsonData = try {
            objectMapper.readValue(file)
        } catch (e: Exception) {
            println("Błąd: ${e.message}")
            return -1
        }
        return jsonData.fragments.count { it.embeddedContent == null || it.embeddedContent.isEmpty() }
    }


    fun generateEmbeddingsForJson(inputFilePath: String, outputFile: String): Boolean {
        return try {
            val inputFile = File(inputFilePath)
            if (!inputFile.exists()) {
                println("Plik nie istnieje.")
                return false
            }
            val rawChunks = inputFile.readLines()

            rawChunks.forEach { chunk ->
                if (chunk.isNotBlank()) {
                    flaskServerService.enqueue(chunk.trim())
                }
            }

            flaskServerService.startServer()

            println("Dodano ${rawChunks.count { it.isNotBlank() }} fragmentów do kolejki.")
            true
        } catch (e: Exception) {
            println("Błąd: ${e.message}")
            false
        }
    }



}
