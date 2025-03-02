package com.owomeb.backend._5gbemowobackend.random

/*
@Component
class LlamaStartupRunner : CommandLineRunner {

    private val serverPath = "C:\\Users\\Pc\\llama.cpp\\build-cuda\\bin\\Release\\llama-server.exe"

    private val modelPath = "C:\\Users\\Pc\\llama.cpp\\models\\Meta-Llama-3.1-8B-Instruct-bf16.gguf"
    private val workingDirectory = File("C:\\Users\\Pc\\llama.cpp\\build-cuda\\bin\\Release")
    private val serverUrl = "http://127.0.0.1:8080/health"

    override fun run(vararg args: String?) {
        println("\n=== Sprawdzanie serwera Llama 3 ===\n")

        if (isServerRunning()) {
            println("✅ Serwer Llama 3 już działa!")
        } else {
            println("⚠️ Serwer Llama 3 nie działa. Uruchamiam...")
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
            "cmd.exe", "/c", "\"$serverPath\" -m \"$modelPath\" --n-gpu-layers 30 --host 127.0.0.1 --port 8081"
        )


        processBuilder.directory(workingDirectory)
        processBuilder.redirectErrorStream(true)

        try {
            thread { // Uruchamiamy serwer w osobnym wątku, żeby nie blokować aplikacji
                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))

                println("🚀 Serwer Llama 3 uruchomiony. Oczekiwanie na gotowość...")

                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    println(line)
                    if (line!!.contains("listening on")) {
                        println("✅ Serwer gotowy do użycia!")
                        break
                    }
                }

                process.waitFor()
            }
        } catch (e: IOException) {
            println("❌ Błąd uruchamiania serwera: ${e.message}")
        }
    }
}

 */
