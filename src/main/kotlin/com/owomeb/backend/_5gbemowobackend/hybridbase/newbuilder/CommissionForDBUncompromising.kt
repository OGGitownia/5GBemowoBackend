package com.owomeb.backend._5gbemowobackend.hybridbase.newbuilder

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.*
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseStatus
import java.util.concurrent.TimeUnit

class CommissionForDBUncompromising(
    baseId: Long,
    private val appPathsConfig: AppPathsConfig,
    private val normManager: NormManager,
    private val photoExtraction: PhotoExtraction,
    private val markdownManager: FinalMarkdown,
    private val sourceUrl: String
)  : Commission(baseId) {
    private var status: CommissionStatus = CommissionStatus.INITIAL
    private var currentStage = 0

    override fun proceed(baseService: BaseService) {
        try {
            when (currentStage) {
                0 -> {
                    download(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                1 -> {
                    extract(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                2 -> {
                    markdown(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                3 -> {
                    chunk(baseService) {
                        currentStage++
                        proceed(baseService)
                    }
                }
                4 -> {
                    finalizeCommission(baseService)
                }
            }
        } catch (e: Exception) {
            updateStatus(baseService, BaseStatus.FAILED, "Błąd: ${e.message}")
            e.printStackTrace()
        }
    }
    private fun download(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Pobieranie dokumentu")
        normManager.downloadAndExtractNorm(
            normUrl = sourceUrl,
            zipPath = appPathsConfig.getZipPath(baseId.toString()),
            docPath = appPathsConfig.getDocPath(baseId.toString())
        )
        status = CommissionStatus.DOWNLOADED
        onFinish()
    }

    private fun extract(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Extractowanie zdjęć")
        photoExtraction.extract(
            input = appPathsConfig.getDocPath(baseId.toString()),
            outputDocx = appPathsConfig.getExtractedDocx(baseId.toString()),
            outputDir = appPathsConfig.getNormDirectory(baseId.toString()),
            onFinish = onFinish
        )
        status = CommissionStatus.EXTRACTED
    }

    private fun markdown(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Tworzenie markdown")
        markdownManager.doMarkdowning(
            inputPath = appPathsConfig.getExtractedDocx(baseId.toString()),
            outputPath = appPathsConfig.getMarkdownPath(baseId.toString())
        )
        TimeUnit.SECONDS.sleep(1)
        status = CommissionStatus.MARKDOWNED
        onFinish()
    }
    private fun chunk(baseService: BaseService, onFinish: () -> Unit) {
        updateStatus(baseService, BaseStatus.PROCESSING, "Chunking Markdown")
        val chunker = FinalChunker(
            pureMarkdownPath = appPathsConfig.getMarkdownPath(baseId.toString()),
            outputPath = appPathsConfig.getChunkedJsonPath(baseId.toString()),
            minChunkLen = 2000,
            maxChunkLen = 4000
        )
        chunker.process()
        TimeUnit.SECONDS.sleep(1)
        status = CommissionStatus.CHUNKED
        onFinish()
    }
    private fun finalizeCommission(baseService: BaseService) {
        status = CommissionStatus.HYBRID_BASED
        updateStatus(baseService, BaseStatus.READY, "Baza gotowa")
        status = CommissionStatus.DONE
    }
}