package com.owomeb.backend._5gbemowobackend

import org.springframework.stereotype.Component
import org.springframework.beans.factory.DisposableBean
import java.io.*

@Component
class LlamaShutdownManager : DisposableBean {

    override fun destroy() {
        println("\n=== Zamykanie modelu Llama 3.1... ===\n")
        stopLlama()
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
