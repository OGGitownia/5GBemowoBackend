package com.owomeb.backend._5gbemowobackend.lamoServices

import org.springframework.context.ApplicationListener
import org.springframework.context.event.ContextClosedEvent
import org.springframework.stereotype.Component
import java.io.BufferedReader
import java.io.InputStreamReader

@Component
class LlamaShutdownRunner : ApplicationListener<ContextClosedEvent> {

    override fun onApplicationEvent(event: ContextClosedEvent) {
        println("\n=== Aplikacja się zamyka, zatrzymuję serwer Llama 3... ===\n")
        stopLlamaServer()
    }

    private fun stopLlamaServer() {
        try {
            val process = Runtime.getRuntime().exec("tasklist")
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                if (line!!.contains("llama-server.exe")) {
                    println("Znaleziono działający proces Llama 3: $line")
                    Runtime.getRuntime().exec("taskkill /F /IM llama-server.exe")
                    println(" Serwer Llama 3 został zatrzymany!")
                    return
                }
            }

            println("ℹNie znaleziono działającego procesu Llama 3.")

        } catch (e: Exception) {
            println("Błąd przy zatrzymywaniu serwera: ${e.message}")
        }
    }
}
