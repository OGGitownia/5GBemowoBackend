package com.owomeb.backend._5gbemowobackend.hybridbase.newbuilder

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.*
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseCreatingMethods
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.LinkedBlockingQueue

@Service
class CommissionManager(
    private val appPathsConfig: AppPathsConfig,
    private val normManager: NormManager,
    private val markdownManager: FinalMarkdown,
    private val embeddingManager: NewEmbeddingManager,
    private val hybridDbCreator: HybridDbCreator,
    private val photoExtraction: PhotoExtraction
) {
    private lateinit var baseService: BaseService

    private val queue = LinkedBlockingQueue<Commission>()

    @PostConstruct
    fun startWorker() {
        Thread {
            while (true) {
                val commission = queue.take()
                try {
                    commission.proceed(baseService)
                } catch (e: Exception) {
                    println("[ERROR] Failed to execute commission: ${e.message}")
                    e.printStackTrace()
                }
            }
        }.start()
    }

    fun submitCommission(baseId: Long, sourceUrl: String, method: BaseCreatingMethods) {
        val commission = when (method) {
            BaseCreatingMethods.FITTED_OVERLAP -> CommissionForDBWithFittedOverlap(
                baseId = baseId,
                sourceUrl = sourceUrl,
                appPathsConfig = appPathsConfig,
                normManager = normManager,
                markdownManager = markdownManager,
                embeddingManager = embeddingManager,
                hybridDbCreator = hybridDbCreator,
                photoExtraction = photoExtraction
            )
            BaseCreatingMethods.UNCOMPROMISINGNESS -> CommissionForDBUncompromising(
                baseId = baseId,
                sourceUrl = sourceUrl,
                appPathsConfig = appPathsConfig,
                normManager = normManager,
                markdownManager = markdownManager,
                photoExtraction = photoExtraction
            )
        }
        queue.put(commission)
    }
    @Autowired
    fun setBaseService(baseService: BaseService) {
        this.baseService = baseService
    }

}
