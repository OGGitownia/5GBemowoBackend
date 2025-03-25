package com.owomeb.backend._5gbemowobackend.core

import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

@Component
class ServerStartupManager(
) {
    private val zipPath = "src/main/resources/norms3/norma.zip"
    private val markdownPath = "src/main/resources/norms3/36331-e60.md"
    private val pureMarkdownPath = "src/main/resources/norms3/36331-e60_pure.md"
    private val docPath = "src/main/resources/norms3/36331-e60.doc"
    private val jsonPath = "src/main/resources/norms3/36331-e60.json"
    private val jsonPath2 = "src/main/resources/norms3/36331-e60.json2"
    private val chunkyPath = "src/main/resources/norms3/chunky.json2"
    private val normaUrl = "https://www.3gpp.org/ftp/Specs/archive/36_series/36.331/36331-e60.zip"
    private val embeddedJsonPath = "src/main/resources/norms3/embeeded36331-e60.json"
    private val hybridDatabaseDir = "src/main/resources/hybrid3"
    private var init = false

    fun serverStartup() {
println("Hello World!")
        //commissionService.startCommission(26, "request.sourceUrl")
        //lbaseController.deleteBaseBySourceUrl("https://www.3gpp.org/ftp/Specs/archive/36_series/36.331/36331-e60.zip")
    }



    private fun resetServer() {

        listOf(markdownPath, docPath, jsonPath, zipPath, embeddedJsonPath).forEach { path ->
            val file = File(path)
            if (file.exists()) {
                if (!file.delete()) {
                    println("Błąd: Nie udało się usunąć pliku: $path")
                    exitProcess(1)
                }
            }
        }

        val hybridDir = File(hybridDatabaseDir)
        if (hybridDir.exists()) {
            hybridDir.deleteRecursively().also { success ->
                if (!success) {
                    println("Błąd: Nie udało się usunąć katalogu: $hybridDatabaseDir")
                    exitProcess(1)
                }
            }
        }
        init = true
        serverStartup()
    }
    @EventListener(ApplicationReadyEvent::class)
    fun onServerStart() {
        serverStartup()
    }


}
