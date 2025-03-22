package com.owomeb.backend._5gbemowobackend.appControllers

import com.owomeb.backend._5gbemowobackend.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.baseCreators.*
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchManagerController
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AsyncBaseProcessor(
    private val baseService: BaseService,
    private val appPathsConfig: AppPathsConfig,
    private val normaManager: NormManager,
    private val markDownManager: MarkdownManager,
    private val embeddingManager: NewEmbeddingManager,
    private val hybridSearchManagerController: HybridSearchManagerController,
    private val hybridSearchService: HybridSearchService,
) {

    @Async
    fun processBase(baseId: Long, sourceUrl: String) {
        try {
            println("Processing $baseId")
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "Pobieranie dokumentu")
            normaManager.downloadAndExtractNorm(sourceUrl, zipPath = appPathsConfig.getZipPath(baseId.toString()),
                docPath = appPathsConfig.getDocPath(baseId.toString()),)
            TimeUnit.SECONDS.sleep(2)

            // 2. Robienie markdown
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "robienie markdown")
            markDownManager.convertDocToMarkdown(
                docPath = appPathsConfig.getDocPath(baseId.toString()),
                markdownPath = appPathsConfig.getMarkdownPath(baseId.toString()),
                pureMarkdownPath = appPathsConfig.getPureMarkdownPath(baseId.toString())
            )
            TimeUnit.SECONDS.sleep(5)



            // 3. Robienie markdown
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "robienie chuk chunk")
            val chunker = ChunkyChunker(
                pureMarkdownPath = appPathsConfig.getPureMarkdownPath(baseId.toString()),
                outputPath = appPathsConfig.getChunkedJsonPath(baseId.toString())
            )
            chunker.chunkThat()
            TimeUnit.SECONDS.sleep(3)

            // 4. Generowanie embeddin embeddin
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "Generowanie embeddin embeddin")
            embeddingManager.generateEmbeddingsForJson(
                inputFilePath = appPathsConfig.getChunkedJsonPath(baseId.toString()),
                outputFile = appPathsConfig.getEmbeddedJsonPath(baseId.toString())
            )

/*
            // 5. tworzenie bazy hybrydowej
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "tworzenie bazdy hybrydowej")
            hybridSearchManagerController.addCommission(
                embeddedJsonPath = appPathsConfig.getEmbeddedJsonPath(baseId.toString()),
                hybridDatabaseDir = appPathsConfig.getHybridBaseDirectory(baseId.toString()))


 */
            TimeUnit.SECONDS.sleep(5)
            // 6. Zakończono
            baseService.updateStatus(baseId, BaseStatus.READY, "Baza gotowa")

        } catch (e: Exception) {
            baseService.updateStatus(baseId, BaseStatus.FAILED, "Błąd przetwarzania: ${e.message}")
            e.printStackTrace()
        }
    }
}
