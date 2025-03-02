package com.owomeb.backend._5gbemowobackend.random

import org.springframework.stereotype.Service
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader

@Service
class FaissDBCreator {

    private val pythonPath = "python"
    private val scriptPath = "create_faiss.py"

    fun isFaissDatabaseExists(): Boolean {
        return try {
            val processBuilder = ProcessBuilder(pythonPath, scriptPath, "check")
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            reader.readLine() == "EXISTS"
        } catch (e: Exception) {
            false
        }
    }

    fun createFaissDatabase(markdownPath: String) {
        if (isFaissDatabaseExists()) {
            println(" Baza FAISS już istnieje.")
            return
        }

        if (!File(markdownPath).exists()) {
            println(" Błąd: Plik Markdown nie istnieje.")
            return
        }

        println(" Tworzenie bazy FAISS na podstawie: $markdownPath")

        try {
            val processBuilder = ProcessBuilder(pythonPath, scriptPath, markdownPath)
            processBuilder.redirectErrorStream(true)
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                println(line)
            }

            process.waitFor()
            println(" Baza FAISS została utworzona!")

        } catch (e: Exception) {
            println(" Błąd tworzenia FAISS: ${e.message}")
        }
    }
}
