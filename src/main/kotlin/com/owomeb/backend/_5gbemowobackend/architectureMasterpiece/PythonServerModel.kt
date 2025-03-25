package com.owomeb.backend._5gbemowobackend.architectureMasterpiece

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.*
import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.web.bind.annotation.*
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.URL
import kotlin.concurrent.thread


abstract class PythonServerModel<T>(
    private val scriptPath: String,
    serverName: String? = null,
    private val autoClose: Boolean = false
) {
    @Volatile
    private var isServerReady = false
    private var process: Process? = null

    private val _queue = MutableStateFlow<List<T>>(emptyList())
    protected val queue: StateFlow<List<T>> = _queue.asStateFlow()


    private var actualPort: Int = findAvailablePort()
    val serverName: String = serverName?.takeIf { isUniqueName(it) } ?: generateUniqueName()

    init {
        ListOfAllActiveInnerServers.registerServer(this)

    }

    private fun startServer() {
        thread {
            try {
                val processBuilder = ProcessBuilder("python", scriptPath, serverName, actualPort.toString())
                processBuilder.redirectErrorStream(true)
                process = processBuilder.start()
                val reader = process!!.inputStream.bufferedReader()

                reader.useLines { lines ->
                    lines.forEach { line -> println("[PYTHON SERVER - $serverName]: $line") }
                }

                val exitCode = process!!.waitFor()
                println("Serwer $serverName zakończył działanie z kodem: $exitCode")

            } catch (e: Exception) {
                println("Błąd uruchamiania serwera $serverName: ${e.message}")
            }
        }
    }

    fun stopServer() {
        try {
            val url = URL("http://localhost:$actualPort/$serverName/shutdown")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()

            if (connection.responseCode == 200) {
                println("Serwer $serverName na porcie $actualPort został zatrzymany.")
            } else {
                println("Nie udało się zatrzymać serwera $serverName na porcie $actualPort. Kod odpowiedzi: ${connection.responseCode}")
            }

            connection.disconnect()
        } catch (e: Exception) {
            println("Błąd podczas zatrzymywania serwera $serverName na porcie $actualPort: ${e.message}")
        } finally {
            process?.destroy()
            println("Proces serwera $serverName na porcie $actualPort został zakończony.")
        }
    }



    open fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        val port = body["port"] as? Int ?: return mapOf("status" to "ERROR", "message" to "Brak portu w żądaniu!")

        actualPort = port


        println("markServerAsReady, długość kolejki: ${queue.value.size}")
        isServerReady = true
        thread(start = true) {
            Thread.sleep(5000)
            processQueueIfNeeded()
        }

        return mapOf("status" to "OK", "message" to "Serwer $serverName gotowy na porcie $actualPort.")
    }


    fun addToQueue(item: T) {
        _queue.value += item
        processQueueIfNeeded()
    }

    private fun processQueueIfNeeded() {
        if (_queue.value.isNotEmpty()) {
            if (isServerReady) {
                processQueue()
            } else {
                println("Próba startu")
                startServer()
            }
        } else if (autoClose && isServerReady) {
            stopServer()
        }
    }

    open fun processQueue() {
        if (_queue.value.isNotEmpty()) {
            val item = _queue.value.first()
            sendRequestToPython(item) { result ->
                publishResult(result, item)
                ////
                _queue.value = _queue.value.drop(1)
                processQueueIfNeeded()
            }
        }
    }

    private fun sendRequestToPython(item: T, callback: (String) -> Unit) {
        thread {
            try {
                val url = URL("http://localhost:$actualPort/$serverName/process")
                println("Piastów")
                println(url)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")


                connection.connectTimeout = 60_000 // 60s
                connection.readTimeout = 120 * 60 * 1000 // 2 godziny


                val objectMapper = jacksonObjectMapper()
                val requestBody = objectMapper.writeValueAsString(item)

                println(requestBody)
                connection.outputStream.write(requestBody.toByteArray(Charsets.UTF_8))
                connection.outputStream.flush()

                val responseCode = connection.responseCode
                val response = connection.inputStream.bufferedReader().use { it.readText() }

                if (responseCode == 200) {
                    callback(response)
                } else {
                    println("Błąd przetwarzania w serwerze $serverName: $response")
                }
            } catch (e: Exception) {
                println("Błąd komunikacji z serwerem $serverName: ${e.message}")
            }
        }
    }



    open fun publishResult(result: String, item: T) {
        println("Item was: $item")
        println("Wynik przetwarzania z serwera $serverName: $result")
    }

    private fun findAvailablePort(): Int {
        ServerSocket(0).use { return it.localPort }
    }

    private fun isUniqueName(name: String): Boolean {
        return ListOfAllActiveInnerServers.value.none { it.serverName == name }
    }

    private fun generateUniqueName(): String {
        var counter = 1
        var generatedName: String
        do {
            generatedName = "Server_$counter"
            counter++
        } while (!isUniqueName(generatedName))
        return generatedName
    }
}

object ListOfAllActiveInnerServers : ApplicationListener<ContextClosedEvent> {
    val value: MutableList<PythonServerModel<*>> = mutableListOf()

    override fun onApplicationEvent(event: ContextClosedEvent) {
        println("Aplikacja Spring Boot jest zamykana. Zatrzymuję serwery Python...")
        value.forEach { it.stopServer() }
        value.clear()
        println("Wszystkie serwery Python zostały zatrzymane.")
    }

    fun registerServer(server: PythonServerModel<*>) {
        if (value.any { it.serverName == server.serverName }) {
            throw IllegalArgumentException("Serwer o nazwie '${server.serverName}' już istnieje!")
        }
        value.add(server)
    }
}
