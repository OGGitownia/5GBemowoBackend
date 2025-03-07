package com.owomeb.backend._5gbemowobackend.hybridsearch

import kotlinx.coroutines.flow.MutableStateFlow
import java.io.File
import org.springframework.web.bind.annotation.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import java.net.HttpURLConnection
import java.net.URL

@RestController
@RequestMapping("/hybrid")
class HybridSearchManagerController() {

    private val queue = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    private val serverState = MutableStateFlow(false)
    private var pythonProcess: Process? = null

    init {
        observeStateChanges()
    }

    @PostMapping("/server-started")
    fun setServerRunning() {
        serverState.value = true
    }

    @PostMapping("/build-complete")
    fun hybridDbCreated(@RequestBody payload: Map<String, String>): Map<String, String> {
        val message = payload["message"] ?: "Brak wiadomości"
        println("Otrzymano informację o utworzeniu bazy hybrydowej: $message")

        return mapOf("status" to "OK", "receivedMessage" to message)
    }

    fun addCommission(embeddedJsonPath: String, hybridDatabaseDir: String) {
        if (!isJsonFile(embeddedJsonPath)) {
            println("Błąd: Plik JSON nie istnieje lub ma zły format! ($embeddedJsonPath)")
            return
        }

        prepareHybridDatabaseDir(hybridDatabaseDir)

        val currentPaths = queue.value.toMutableList()
        currentPaths.add(Pair(embeddedJsonPath, hybridDatabaseDir))
        queue.value = currentPaths
    }

    private fun isJsonFile(filePath: String): Boolean {
        val file = File(filePath)
        return file.exists() && file.isFile && file.extension.lowercase() == "json"
    }

    private fun prepareHybridDatabaseDir(dirPath: String) {
        val directory = File(dirPath)

        if (!directory.exists()) {
            directory.mkdirs()
            println("Utworzono katalog: $dirPath")
        } else {
            val files = directory.listFiles()
            if (files != null && files.isNotEmpty()) {
                files.forEach { it.delete() }
                println("Wyczyszczono katalog: $dirPath")
            }
        }
    }

    private fun observeStateChanges() {
        CoroutineScope(Dispatchers.Default).launch {
            queue
                .map { it.size }
                .distinctUntilChanged()
                .collect { queueSize ->
                    if (queueSize > 0 && serverState.value) {
                        handleQueue()
                    }
                }
        }

        CoroutineScope(Dispatchers.Default).launch {
            queue
                .map { it.isNotEmpty() }
                .distinctUntilChanged()
                .collect { isNotEmpty ->
                    if (isNotEmpty && !serverState.value && pythonProcess == null) {
                        startPythonWhoBaseCreatorIs()
                    }
                }
        }

        CoroutineScope(Dispatchers.Default).launch {
            serverState.collect { isRunning ->
                if (isRunning && queue.value.isNotEmpty()) {
                    handleQueue()
                }
            }
        }
    }




    private fun handleQueue() {
        println("Próba obsłużenia kolejki")
        val item = queue.value.firstOrNull() ?: return

        try {

            val url = URL("http://192.168.0.148:5000/handle-queue")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")

            val payload = """{"jsonPath": "${item.first}", "hybridDbPath": "${item.second}"}"""
            connection.outputStream.use { it.write(payload.toByteArray()) }

            val responseCode = connection.responseCode
            if (responseCode == 200) {
                println("Element zgłoszony do serwera")
            } else {
                println("Python odrzucił zgłoszenie $responseCode")
            }

        } catch (e: Exception) {
            println("Błąd komunikacji z serwerem Python: ${e.message}")
        }
    }


    fun startPythonWhoBaseCreatorIs() {
        if (pythonProcess != null && pythonProcess!!.isAlive) {
            println("Serwer Python już działa! Nie uruchamiam ponownie.")
            return
        }

        pythonProcess?.destroy()
        pythonProcess = null

        try {
            val processBuilder = ProcessBuilder("python", "src/main/resources/pythonScripts/hybridsearch/server.py")
            processBuilder.redirectErrorStream(true)
            pythonProcess = processBuilder.start()

            // Obsługa logów serwera Flask
            Executors.newSingleThreadExecutor().submit {
                try {
                    BufferedReader(InputStreamReader(pythonProcess!!.inputStream)).use { reader ->
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            println("[FlaskHybridToyota] $line")
                        }
                    }
                } catch (e: Exception) {
                    println("Błąd podczas czytania logów Pythona: ${e.message}")
                }
            }

            println("Serwer Python został uruchomiony!")

        } catch (e: Exception) {
            println("Błąd podczas uruchamiania serwera Python: ${e.message}")
        }
    }



    fun isHybridBaseExists(hybridDatabaseDir: String): Boolean {
        val directory = File(hybridDatabaseDir)

        if (!directory.exists()) {
            directory.mkdirs()
            return false
        }

        val files = directory.listFiles()
        return files != null && files.isNotEmpty()
    }

    fun shutdownServer() {
        try {
            // Wysyłamy żądanie do Flask, aby się zamknął
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

        // Teraz możemy zatrzymać proces JVM
        pythonProcess?.let {
            if (it.isAlive) {
                it.destroy()
                it.waitFor()
                println("Proces Python został zakończony.")
            }
        }
        pythonProcess = null
        serverState.value = false
    }




}
