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
            return -1  // Zwraca -1, jeśli plik nie istnieje lub jest pusty
        }

        val objectMapper = jacksonObjectMapper()
        val jsonData: JsonData = try {
            objectMapper.readValue(file)
        } catch (e: Exception) {
            println("❌ Błąd wczytywania pliku JSON: ${e.message}")
            return -1
        }
        return jsonData.fragments.count { it.embeddedContent == null || it.embeddedContent.isEmpty() }
    }


    fun generateEmbeddingsForJson(inputJsonPath: String): Boolean {
        return try {
            val inputFile = File(inputJsonPath)
            if (!inputFile.exists()) {
                println("JSON nie istnieje.")
                return false
            }

            val rawJsonData = Json.decodeFromString<JsonData>(inputFile.readText())

            rawJsonData.fragments.forEach { fragment ->
                flaskServerService.enqueue(fragment.content)
            }
            flaskServerService.startServer()

            println("Dodano ${rawJsonData.fragments.size} fragmentów do kolejki.")

            true
        } catch (e: Exception) {
            println("Błąd i chuj ${e.message}")
            false
        }
    }


}
