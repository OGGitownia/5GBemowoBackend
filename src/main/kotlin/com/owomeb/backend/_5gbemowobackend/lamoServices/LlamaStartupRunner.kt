package com.owomeb.backend._5gbemowobackend.lamoServices

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread


@Component
class LlamaStartupRunner(val llamaService: LlamaService) : CommandLineRunner {

    private val serverPath = "C:\\Users\\Pc\\llama.cpp\\build-cuda\\bin\\Release\\llama-server.exe"

    private val modelPath = "C:\\Users\\Pc\\llama.cpp\\models\\Meta-Llama-3.1-8B-Instruct-bf16.gguf"
    private val workingDirectory = File("C:\\Users\\Pc\\llama.cpp\\build-cuda\\bin\\Release")
    private val serverUrl = "http://127.0.0.1:8080/health"

    override fun run(vararg args: String?) {
        println("\n=== Sprawdzanie serwera Llama 3 ===\n")
        if (isServerRunning()) {
            println("Serwer Llama 3 działa!")
        } else {
            println("Serwer Llama 3 nie działa. Uruchamiam go terazzz")
            startLlamaServer()
        }
    }

    private fun isServerRunning(): Boolean {
        return try {
            val url = URL(serverUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 3000
            connection.readTimeout = 3000

            val responseCode = connection.responseCode
            responseCode == 200
        } catch (e: Exception) {
            false
        }
    }

    private fun startLlamaServer() {
        val processBuilder = ProcessBuilder(
            "cmd.exe", "/c", "\"$serverPath\" -m \"$modelPath\" --n-gpu-layers 26 --host 127.0.0.1 --port 8081"
        )


        processBuilder.directory(workingDirectory)
        processBuilder.redirectErrorStream(true)

        try {
            thread {
                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                println("Serwer Llama 3 uruchomiony. gotuje się")

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println(line)
                    if (line!!.contains("listening on")) {
                        llamaService.ready = true
                        break
                    }
                }

                process.waitFor()
            }
        } catch (e: IOException) {
            println("Błąd: ${e.message}")
        }
    }
}


