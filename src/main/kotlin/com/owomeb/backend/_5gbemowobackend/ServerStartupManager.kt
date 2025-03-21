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
    private val hybridSearchService: HybridSearchService,
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
        println("\n()()()()()()()(()()()()((   Uruchamianie serwera  ()()()()()()()(()()()()((\n")

        if (!normaManager.isNormaDownloaded(docPath)) {
            if(!init){
                println("Plik normy 3GPP nie istnieje")
                resetServer()
                return
            }
            if (!normaManager.downloadAndExtractNorm(normaUrl, zipPath, docPath)) {
                println("Errororr: Nie udało się pobrać normy 3GPP")
                exitProcess(1)
            }
            println("Norma 3GPP pobrana i rozpakowana")
        }

        if (!markDownManager.isMarkdownExists(markdownPath)) {
            if (!init) {
                println("Markdown nie istnieje")
                resetServer()
                return
            } else {
                println("Tworzenie Markdown")
                if (!markDownManager.convertDocToMarkdown(docPath, markdownPath, pureMarkdownPath)) {
                    println("Błąd: Nie udało się przekonwertować DOC albo docX na Markdown")
                    exitProcess(1)
                }
                println("Markdown utworzony")
                val chunker = ChunkyChunker(pureMarkdownPath, chunkyPath)
                chunker.findPossibleChapters()
            }
        }



        /*
        if (!jsonManager.isJsonExists(jsonPath)) {
            println("Brak pliku JSON z chunk")
            if (!init) {
                resetServer()
                return
            } else {
                if (!jsonManager.createJson(docPath, jsonPath)) {
                    println("Błąd: Nie udało się utworzyć pliku JSON z chunk")
                    exitProcess(1)
                }
                println("JSON utworzony")
            }
        }

         */


        if (!embeddingManager.isEmbeddedJsonExist(embeddedJsonPath)) {
            if (!init) {
                println("embedded for json nie istnieje. Czyszczenie serwera")
                resetServer()
                return
            } else {
                println("Tworzenie embedded JSON")
                if (!embeddingManager.generateEmbeddingsForJson(chunkyPath)) {
                    println("Błąd: Nie udało się przekonwertować json na json. Czysty absurd")
                    exitProcess(1)
                }
                /*
                var a = embeddingManager.countFragmentsWithoutEmbedding(jsonPath)
                ///Thread.sleep(10000)
                while(a > 0){
                    a = embeddingManager.countFragmentsWithoutEmbedding(jsonPath)
                    println("FRAGMENTS without emb: ${a}")
                    Thread.sleep(2000)
                }
                
                 */
            }
        }

        if (!hybridSearchManagerController.isHybridBaseExists(hybridDatabaseDir)) {
            hybridSearchManagerController.addCommission(embeddedJsonPath, hybridDatabaseDir)
        }
        if (hybridSearchManagerController.isHybridBaseExists(hybridDatabaseDir)) {
            //Stats.getStats(jsonPath, embeddedJsonPath)
            println("\nBaza ok\n")
            hybridSearchService.addToQueue("What is suspended RRC connection?")
            //hybridSearchService.addToQueue("What is the role of the schedulingInfoSIB1 field in MIB-NB?")
            val contexts = listOf(
                    "",
            "Opis śmierci przedstawiony w Boskiej Komedii Dantego Alighieri.",
            "Ciekawostka o sarnach: Sarny potrafią komunikować się za pomocą dźwięków, w tym ostrzegawczych gwizdów."
            )

            val questions = listOf(
                "cześć jak się masz moja droga llamo?",
                "Jak Dante opisuje piekło w Boskiej Komedii i jakie są jego najcięższe kary?",
                "W jaki sposób sarny komunikują się w stadzie i jakie dźwięki wydają?"
            )
            for (i in contexts.indices) {
                //llamaService.addToQueue(questions[i], contexts[i])
            }
        }

       // jsonManager.processDocToTxt(docPath, jsonPath2)




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
