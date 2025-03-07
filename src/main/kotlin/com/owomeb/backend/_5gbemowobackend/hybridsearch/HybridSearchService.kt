package com.owomeb.backend._5gbemowobackend.hybridsearch

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.*
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

@Service
@RestController
@RequestMapping("/hybrid-service")
class HybridSearchService {

    private val queryQueue = mutableListOf<String>()
    private val _isServerReady = MutableStateFlow(false)
    val isServerReady = _isServerReady.asStateFlow()


    fun startSearchServer(): Map<String, String> {
        println("Uruchamiam serwer wyszukiwania w bazie hybrydowej")

        thread {
            val processBuilder = ProcessBuilder(
                "python", "src/main/resources/pythonScripts/hybridsearch/searchEngine.py"
            )
            processBuilder.redirectErrorStream(true)
            processBuilder.start()
        }

        return mapOf("status" to "STARTED", "message" to "Serwer wyszukiwania został uruchomiony w tle.")
    }

    @PostMapping("/search-server-ready")
    fun searchServerReady(): Map<String, String> {
        println(" Serwer wyszukiwania w bazie hybrydowej jest gotowy!")
        _isServerReady.value = true

        processQueryQueue()
        return mapOf("status" to "OK", "message" to "Serwer wyszukiwania gotowy.")
    }

    @PostMapping("/search")
    fun search(@RequestBody query: Map<String, String>): Map<String, String> {
        val queryText = query["query"] ?: return mapOf("error" to "Brak zapytania")

        if (_isServerReady.value) {
            return sendQueryToPython(queryText)
        } else {
            println(" Serwer nie jest gotowy, dodaję zapytanie do kolejki: $queryText")
            queryQueue.add(queryText)
            return mapOf("status" to "PENDING", "message" to "Zapytanie dodane do kolejki.")
        }
    }

    /**
     * Przetwarza kolejkę zapytań, gdy serwer wyszukiwania jest gotowy.
     */
    private fun processQueryQueue() {
        println("🚀 Rozpoczynam przetwarzanie kolejki zapytań...")
        val iterator = queryQueue.iterator()

        while (iterator.hasNext()) {
            val query = iterator.next()
            val response = sendQueryToPython(query)

            if (response["status"] == "OK") {
                iterator.remove() // Usuwamy zapytanie tylko po otrzymaniu odpowiedzi
            }
        }
    }

    /**
     * Wysyła zapytanie do serwera Pythona
     */
    private fun sendQueryToPython(query: String): Map<String, String> {
        return try {
            val url = URL("http://localhost:5001/search")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true

            val requestBody = """{"query": "$query"}""".toByteArray()
            connection.outputStream.write(requestBody)
            connection.outputStream.flush()

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                println("✅ Otrzymano odpowiedź z serwera wyszukiwania dla zapytania: $query")
                mapOf("status" to "OK", "message" to "Zapytanie przetworzone.")
            } else {
                println("⚠️ Serwer wyszukiwania zwrócił błąd: $responseCode")
                mapOf("status" to "ERROR", "message" to "Błąd po stronie serwera wyszukiwania.")
            }
        } catch (e: Exception) {
            println("❌ Błąd podczas komunikacji z serwerem wyszukiwania: ${e.message}")
            mapOf("status" to "ERROR", "message" to "Błąd komunikacji z serwerem wyszukiwania.")
        }
    }

    fun shutdownServer() {
        try {
            val url = URL("http://localhost:5000/shutdown")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            if (connection.responseCode == 200) {
                println("Serwer Flask został poprawnie zatrzymany.")
            } else {
                println("Nie udało się zatrzymać serwera Flask, kod: ${connection.responseCode}")
            }

            connection.disconnect()
        } catch (e: Exception) {
            println("Błąd podczas wysyłania żądania do Flask: ${e.message}")
        }
    }
}
