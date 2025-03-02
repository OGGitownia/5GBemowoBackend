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

    fun isEmbeddedJsonExist(embeddedJsonPath: String): Boolean {
        val file = File(embeddedJsonPath)
        if (!file.exists() || file.length() <= 200) {
            println("JSON nie istnieje")
            return false
        }

        val objectMapper = jacksonObjectMapper()
        val jsonData: JsonData = try {
            objectMapper.readValue(file)
        } catch (e: Exception) {
            println("Błąd JSON: ${e.message}")
            return false
        }

        var withEmbedding = 0
        var withoutEmbedding = 0

        jsonData.fragments.forEach { fragment ->
            if (fragment.embeddedContent != null && fragment.embeddedContent.isNotEmpty()) {
                withEmbedding++
            } else {
                withoutEmbedding++
            }
        }

        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val statsFile = File("statystykiEmbedded_$timestamp.txt")

        val report = """
        📊 STATYSTYKI EMBEDDED JSON 📊
        Data: $timestamp
        📌 Liczba wszystkich fragmentów: ${jsonData.fragments.size}
        ✅ Z embeddingiem: $withEmbedding
        ❌ Bez embeddingu: $withoutEmbedding
        ---------------------------------------------------
        ${if (withoutEmbedding == 0) "✅ Wszystkie fragmenty mają embedding!" else "⚠️ Brakuje embeddingu w niektórych fragmentach!"}
    """.trimIndent()

        statsFile.writeText(report)
        println("📄 Statystyki zapisane do: ${statsFile.absolutePath}")

        return withoutEmbedding == 0
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

            println("Dodano ${rawJsonData.fragments.size} fragmentów do kolejki.")

            true
        } catch (e: Exception) {
            println("Błąd i chuj ${e.message}")
            false
        }
    }


}
