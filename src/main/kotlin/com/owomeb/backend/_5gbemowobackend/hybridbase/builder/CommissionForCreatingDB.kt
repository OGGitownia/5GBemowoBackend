package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseStatus
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
import java.util.concurrent.TimeUnit




class CommissionForCreatingDB(private val baseService: BaseService,
                              private val appPathsConfig: AppPathsConfig,
                              private val normaManager: NormManager,
                              private val markDownManager: MarkdownManager,
                              private val embeddingManager: NewEmbeddingManager,
                              private val hybridDbCreator: HybridDbCreator,
                              private val hybridSearchService: HybridSearchService,
                              val baseID: Long,
                              val sourceUrl: String,
                              commissionStatus: CommissionStatus = CommissionStatus.INITIAL
) {
    init {
        download()
    }

    var commissionStatus: CommissionStatus = commissionStatus
        set(value) {
            if (field != value) {
                println("CommissionStatus changed from $field to $value")
                field = value

                when (value) {
                    CommissionStatus.DOWNLOADED -> markdown()
                    CommissionStatus.MARKDOWNED -> chunk()
                    CommissionStatus.CHUNKED -> embed()
                    CommissionStatus.EMBEDDED -> hybridBase()
                    CommissionStatus.HYBRID_BASED -> finalize()
                    CommissionStatus.DONE -> println("Processing completed for $baseID.")
                    CommissionStatus.INITIAL -> {}
                }
            }
        }

    private fun download() {
        println("Downloading for $baseID...")
        baseService.updateStatus(baseID, BaseStatus.PROCESSING, "Pobieranie dokumentu")
        normaManager.downloadAndExtractNorm(sourceUrl, zipPath = appPathsConfig.getZipPath(baseID.toString()),
            docPath = appPathsConfig.getDocPath(baseID.toString())
        )
        commissionStatus = CommissionStatus.DOWNLOADED
    }

    private fun markdown() {
        println("Markdowning for $baseID...")
        baseService.updateStatus(baseID, BaseStatus.PROCESSING, "robienie markdown")
        markDownManager.convertDocToMarkdown(
            docPath = appPathsConfig.getDocPath(baseID.toString()),
            markdownPath = appPathsConfig.getMarkdownPath(baseID.toString()),
            pureMarkdownPath = appPathsConfig.getPureMarkdownPath(baseID.toString())
        )
        TimeUnit.SECONDS.sleep(5)
        commissionStatus = CommissionStatus.MARKDOWNED
    }

    private fun chunk() {
        println("Chunking for $baseID...")
        baseService.updateStatus(baseID, BaseStatus.PROCESSING, "robienie chuk chunk")
        val chunker = ChunkyChunker(
            pureMarkdownPath = appPathsConfig.getPureMarkdownPath(baseID.toString()),
            outputPath = appPathsConfig.getChunkedJsonPath(baseID.toString())
        )
        chunker.chunkThat()
        TimeUnit.SECONDS.sleep(3)
        commissionStatus = CommissionStatus.CHUNKED
    }

    private fun embed() {
        println("Embedding for $baseID...")
        baseService.updateStatus(baseID, BaseStatus.PROCESSING, "Generowanie embeddin embeddin")
        embeddingManager.generateEmbeddingsForJson(
            inputFilePath = appPathsConfig.getChunkedJsonPath(baseID.toString()),
            outputFile = appPathsConfig.getEmbeddedJsonPath(baseID.toString()),
            commission = this
        )
    }

    private fun hybridBase() {
        println("Creating hybrid base for $baseID")
        baseService.updateStatus(baseID, BaseStatus.PROCESSING, "Tworzenie bazy hybrydowej")
        hybridDbCreator.createDb(
            inputFilePath = appPathsConfig.getEmbeddedJsonPath(baseID.toString()),
            outputFilePath = appPathsConfig.getHybridBaseDirectory(baseID.toString()),
            commission = this
        )
    }

    private fun finalize() {
        println("Finalizing for $baseID...")
        baseService.updateStatus(baseID, BaseStatus.READY, "Baza gotowa")
        commissionStatus = CommissionStatus.DONE
    }
}




enum class CommissionStatus {
    INITIAL,
    DOWNLOADED,
    MARKDOWNED,
    CHUNKED,
    EMBEDDED,
    HYBRID_BASED,
    DONE,
}

