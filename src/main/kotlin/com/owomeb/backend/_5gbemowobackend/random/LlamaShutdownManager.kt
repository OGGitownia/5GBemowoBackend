package com.owomeb.backend._5gbemowobackend.random

import com.owomeb.backend._5gbemowobackend.baseCreators.FlaskServerService
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchManagerController
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchService
import org.springframework.stereotype.Component
import org.springframework.beans.factory.DisposableBean
import java.io.*
import java.net.HttpURLConnection
import java.net.URL

@Component
class LlamaShutdownManager(private val flaskServerService: FlaskServerService,
                           private val hybridSearchManagerController: HybridSearchManagerController) : DisposableBean {

    override fun destroy() {
        println("\n=== Zamykanie modelu Llama 3.1... ===\n")

        stopHybridToyota()
        stopLlama()
        stopEmbedding()
    }

    fun stopEmbedding() {
        println(" Zatrzymywanie serwera embeddingów...")

        val response = flaskServerService.shutdownServer()

        println(" Odpowiedź z Flaska: $response")
    }

    fun stopHybridToyota(){
        println(" Zatrzymywanie servisu Toyoty")

        hybridSearchManagerController.shutdownServer()
    }

    private fun stopLlama() {
        try {
            // Zabijamy proces llama-cli.exe, jeśli działa
            val processBuilder = ProcessBuilder("taskkill", "/F", "/IM", "llama-cli.exe")
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println("[LlamaShutdown] $line")
            }

            process.waitFor()
            println("\n✅ Model Llama został zamknięty i VRAM zwolniony.\n")

        } catch (e: IOException) {
            println("❌ Błąd podczas zamykania modelu: ${e.message}")
        }
    }
}
