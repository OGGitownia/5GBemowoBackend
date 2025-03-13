package com.owomeb.backend._5gbemowobackend.baseCreators

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
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
    private val embeddedJsonPath = "src/main/resources/norms/embeeded36331-e60.json"

    private val _queue = MutableStateFlow<List<String>>(emptyList())
    val queue: StateFlow<List<String>> = _queue.asStateFlow()

    private val _serverReady = MutableStateFlow(false)
    val serverReady: StateFlow<Boolean> = _serverReady.asStateFlow()

    init {
        coroutineScope.launch {
            combine(queue, serverReady) { queue, serverReady ->
                queue.isNotEmpty() && serverReady
            }.collect { canProcess ->
                if (canProcess) processQueue(embeddedJsonPath)
            }
        }

        /*
        coroutineScope.launch {
            queue.collect { currentQueue ->
                if (currentQueue.isNotEmpty() && !_serverReady.value) {
                    println("PIA1")
                    startServer()
                }
            }
        }

         */
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

    fun getQueueSize(): Int{
        return queue.value.size
    }


    private fun processQueue(embeddedPath: String) {
        coroutineScope.launch {
            proceedParts++

            val embeddedFile = File(embeddedPath)
            val newJsonData = JsonData(mutableListOf())

            while (_queue.value.isNotEmpty()) {
                val textToProcess = _queue.value.firstOrNull() ?: continue
                //println(" Wysyłanie 1 fragmentu do serwera Flask:")

                println("Aktualny stan kolejki: ${getQueueSize()}")
                if(getQueueSize() == 1300){
                    println(newJsonData)
                }
                try {
                    val embedding = withContext(Dispatchers.IO) { getEmbeddings(textToProcess) }

                    if (embedding != null) {
                        newJsonData.fragments.add(Fragment(textToProcess, embedding))
                        _queue.update { it.drop(1) }
                    } else {
                        println("Błąd: Flask nie zwrócił poprawnych danych dla: $textToProcess")
                        System.out.flush()
                    }
                } catch (e: Exception) {
                    println("Błąd podczas komunikacji z Flask: ${e.message}")
                    System.out.flush()
                }
            }

            embeddedFile.writeText(Json.encodeToString(newJsonData))
            println("Zapisano nowy plik JSON: $embeddedPath")
        }
    }



    fun startServer() {
        if (_serverReady.value) return

        try {
            val processBuilder = ProcessBuilder("python", "src/main/resources/pythonScripts/embedding.py")
            processBuilder.redirectErrorStream(true)
            flaskProcess = processBuilder.start()

            Executors.newSingleThreadExecutor().submit {
                BufferedReader(InputStreamReader(flaskProcess!!.inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        println("[Flask] $line")
                    }
                }
            }
        } catch (e: Exception) {
            throw RuntimeException("Nie udało się uruchomić serwera Flask", e)
        }
        val json1 = convertQueueToJson()


    }

    fun convertQueueToJson(): String {
        return runBlocking {
            val queueList = queue.first()
            Json.encodeToString(queueList)
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

            response.body?.get("embedding")
        } catch (e: Exception) {
            println("Błąd wysyłania do Flask: ${e.message}")
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
