package com.owomeb.backend._5gbemowobackend

import com.owomeb.backend._5gbemowobackend.baseCreators.*
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchManagerController
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchService
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

@Component
class ServerStartupManager(
    private val normaManager: NormManager,
    private val jsonManager: JSONManager,
    private val markDownManager: MarkdownManager,
    private val embeddingManager: EmbeddingManager,
    private val hybridSearchManagerController: HybridSearchManagerController,
    private val hybridSearchService: HybridSearchService
) {
    private val zipPath = "src/main/resources/norms/norma.zip"
    private val markdownPath = "src/main/resources/norms/36331-e60.md"
    private val docPath = "src/main/resources/norms/36331-e60.doc"
    private val jsonPath = "src/main/resources/norms/36331-e60.json"
    private val normaUrl = "https://www.3gpp.org/ftp/Specs/archive/36_series/36.331/36331-e60.zip"
    private val embeddedJsonPath = "src/main/resources/norms/embeeded36331-e60.json"
    private val hybridDatabaseDir = "src/main/resources/hybrid"
    private var init = false

    fun serverStartup() {
        println("\n()()()()()()()(()()()()((   Uruchamianie serwera  ()()()()()()()(()()()()((\n")


        if (!normaManager.isNormaDownloaded(docPath)) {
            if(!init){
                println("Plik normy 3GPP nie istnieje. Czyszczenie serwera i pobieranie...")
                resetServer()
                return
            }
            if (!normaManager.downloadAndExtractNorm(normaUrl, zipPath, docPath)) {
                println("Błąd: Nie udało się pobrać normy 3GPP.")
                exitProcess(1)
            }
            println("Norma 3GPP pobrana i rozpakowana.")
        }


        if (!jsonManager.isJsonExists(jsonPath)) {
            if (!init) {
                println("Brak pliku JSON. Czyszczenie serwera...")
                resetServer()
                return
            } else {
                println("JSON nie istnieje. Tworzenie JSON...")
                if (!jsonManager.createJson(docPath, jsonPath)) {
                    println("Błąd: Nie udało się utworzyć JSON.")
                    exitProcess(1)
                }
                println("JSON utworzony.")
            }
        }


        if (!embeddingManager.isEmbeddedJsonExist(embeddedJsonPath)) {
            if (!init) {
                println("embedded for json nie istnieje. Czyszczenie serwera...")
                resetServer()
                return
            } else {
                println("Tworzenie embedded JSON...")
                if (!embeddingManager.generateEmbeddingsForJson(jsonPath)) {
                    println("Błąd: Nie udało się przekonwertować json na json. Czysty absurd")
                    exitProcess(1)
                }
                /*
                var a = embeddingManager.countFragmentsWithoutEmbedding(jsonPath)
                Thread.sleep(10000)
                while(a > 0){
                    a = embeddingManager.countFragmentsWithoutEmbedding(jsonPath)
                    println("FRAGMENTS without emb: ${a}")
                    Thread.sleep(2000)
                }
                
                 */
            }
        }

        if (!markDownManager.isMarkdownExists(markdownPath)) {
            if (!init) {
                println("Markdown nie istnieje. Czyszczenie serwera...")
                resetServer()
                return
            } else {
                println("Tworzenie Markdown...")
                if (!markDownManager.convertDocToMarkdown(docPath, markdownPath)) {
                    println("Błąd: Nie udało się przekonwertować DOC na Markdown.")
                    exitProcess(1)
                }
                println("Markdown utworzony.")
            }
        }

        if (!hybridSearchManagerController.isHybridBaseExists(hybridDatabaseDir)) {
            hybridSearchManagerController.addCommission(embeddedJsonPath, hybridDatabaseDir)
        }
        if (hybridSearchManagerController.isHybridBaseExists(hybridDatabaseDir)) {
            //Stats.getStats(jsonPath, embeddedJsonPath)
            println("\nBaza ok\n")
            hybridSearchService.addToQueue("What is the purpose of RRCConnectionRequest?")
        }




        ///com.owomeb.backend._5gbemowobackend.pythonServerModel.main()
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


}
