package com.owomeb.backend._5gbemowobackend.baseCreators

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.Executors

@Service
class FlaskServerService {
    private var proceedParts = 0
    private val BASE_URL = "http://localhost:5000"
    private var flaskProcess: Process? = null
    private val restTemplate = RestTemplate()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    private val jsonPath = "src/main/resources/norms/36331-e60.json"

    private val _queue = MutableStateFlow<List<String>>(emptyList())
    val queue: StateFlow<List<String>> = _queue.asStateFlow()

    private val _serverReady = MutableStateFlow(false)
    val serverReady: StateFlow<Boolean> = _serverReady.asStateFlow()

    init {
        coroutineScope.launch {
            combine(queue, serverReady) { queue, serverReady ->
                queue.isNotEmpty() && serverReady
            }.collect { canProcess ->
                if (canProcess) processQueue(jsonPath)
            }
        }

        coroutineScope.launch {
            queue.collect { currentQueue ->
                if (currentQueue.isNotEmpty() && !_serverReady.value) {
                    startServer()
                }
            }
        }
        coroutineScope.launch {
            queue.collect { currentQueue ->
                if (currentQueue.isEmpty() && _serverReady.value) {
                    shutdownServer()
                    println("automatyczne zamknięcie servera flask")
                }
            }
        }
    }

    fun enqueue(text: String) {
        _queue.update { it + text }
    }



    private fun processQueue(jsonFilePath :String) {
        coroutineScope.launch {
            proceedParts++
            println("QUEUE SIZE: " + _queue.value.size)
            val jsonFile = File(jsonFilePath)

            val jsonData: JsonData = if (jsonFile.exists() && jsonFile.length() > 0) {
                Json.decodeFromString(jsonFile.readText())
            } else {
                JsonData(mutableListOf())
            }

            while (_queue.value.isNotEmpty()) {
                val textsToProcess = _queue.value.take(1)
                println("🚀 Wysyłanie ${textsToProcess.size} fragmentów do serwera Flask")

                textsToProcess.forEach { text ->
                    val embedding = getEmbeddings(text)

                    jsonData.fragments.find { it.content == text }?.let {
                        if (embedding != null) {
                            it.embeddedContent = embedding
                        }
                    } ?: embedding?.let { Fragment(text, it) }?.let { jsonData.fragments.add(it) }
                }

                jsonFile.writeText(Json.encodeToString(jsonData))

                _queue.update { it.drop(textsToProcess.size) }
            }
        }
    }



    fun startServer() {
        if (_serverReady.value) return

        try {
            val processBuilder = ProcessBuilder("python", "src/main/resources/pythonScripts/embedding.py")
            processBuilder.redirectErrorStream(true)
            flaskProcess = processBuilder.start()

            // Obsługa logów serwera Flask
            Executors.newSingleThreadExecutor().submit {
                BufferedReader(InputStreamReader(flaskProcess!!.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        println("[Flask] $line")
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException(" Nie udało się uruchomić serwera Flask", e)
        }
    }

    fun setServerReady() {
        _serverReady.value = true
    }


    fun getEmbeddings(text: String): List<Float>? {
        val requestBody = mapOf("text" to text)
        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestEntity = HttpEntity(requestBody, headers)

        return try {
            val response = restTemplate.exchange(
                "$BASE_URL/embed",
                HttpMethod.POST,
                requestEntity,
                object : ParameterizedTypeReference<Map<String, List<Float>>>() {}
            )

            response.body?.get("embedding") // Pobiera listę embeddingów
        } catch (e: Exception) {
            println("❌ Błąd wysyłania do Flask: ${e.message}")
            null
        }
    }




    fun shutdownServer(): String {
        val response: ResponseEntity<String> = restTemplate.postForEntity("$BASE_URL/shutdown", null, String::class.java)
        stopServerProcess()
        _serverReady.value = false
        return response.body ?: "Serwer Flask został zamknięty"
    }

    private fun stopServerProcess() {
        flaskProcess?.destroy()
        println("Serwer Flask został zamknięty.")
    }
}
