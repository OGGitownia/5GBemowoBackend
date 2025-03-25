package com.owomeb.backend._5gbemowobackend.deprecated

import org.springframework.stereotype.Component
import org.json.JSONArray
import org.json.JSONObject
import java.nio.file.Files
import java.nio.file.Paths

@Component
object Stats {
    fun getStats(jsonPath: String, embeddedJsonPath: String) {
        try {
            val firstJson = String(Files.readAllBytes(Paths.get(jsonPath))).trim()
            val secondJson = String(Files.readAllBytes(Paths.get(embeddedJsonPath))).trim()

            val firstSize = extractArraySize(firstJson)
            val secondSize = extractArraySize(secondJson)

            println("Liczba elementów w $jsonPath: $firstSize")
            println("Liczba elementów w $embeddedJsonPath: $secondSize")
            println("Różnica: ${kotlin.math.abs(firstSize - secondSize)} elementów")
        } catch (e: Exception) {
            println("Błąd podczas przetwarzania plików JSON: ${e.message}")
        }
    }

    private fun extractArraySize(json: String): Int {
        return when {
            json.startsWith("[") -> JSONArray(json).length()
            json.startsWith("{") -> {
                val jsonObject = JSONObject(json)
                if (jsonObject.has("fragments") && jsonObject.get("fragments") is JSONArray) {
                    jsonObject.getJSONArray("fragments").length()
                } else {
                    throw IllegalArgumentException("Nie znaleziono listy 'fragments' w JSON")
                }
            }
            else -> throw IllegalArgumentException("Nieprawidłowy format JSON")
        }
    }
}
