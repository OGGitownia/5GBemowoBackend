package com.owomeb.backend._5gbemowobackend.lamoServices

import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.concurrent.Executors


class LlamaStartupRunner : CommandLineRunner {

    private val pythonScriptPath = "backend/_scripts/llama_model.py"

    var ready: Boolean = false

    override fun run(vararg args: String?) {
        println("=== Inicjalizacja serwera Llama 3 ===")
        ready = true
        println("Pythonowy model Llama 3 gotowy do użycia!")
    }

    fun generateResponse(prompt: String): String {
        if (!ready) return "Model nie jest gotowy"

        return try {
            println("Wysyłanie zapytania do modelu...")

            val processBuilder = ProcessBuilder("python3", pythonScriptPath)
            processBuilder.redirectErrorStream(true)

            val process = processBuilder.start()

            // Wysyłanie danych do Pythona
            OutputStreamWriter(process.outputStream).use { writer ->
                writer.write(prompt + "\n")
                writer.flush()
            }

            // Oczekiwanie na zakończenie procesu
            process.waitFor()

            // Odczyt odpowiedzi
            val response = BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                reader.readText()
            }

            println("Odpowiedź: $response")
            response
        } catch (e: Exception) {
            println("Błąd generowania odpowiedzi: ${e.message}"+ e)
            "Błąd: Nie udało się wygenerować odpowiedzi"
        }
    }
}
