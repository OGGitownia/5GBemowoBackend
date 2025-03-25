package com.owomeb.backend._5gbemowobackend.hybridbase.builder


import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.*
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service

@Service
class CommissionService(
    private val baseService: BaseService,
    private val appPathsConfig: AppPathsConfig,
    private val normManager: NormManager,
    private val markdownManager: MarkdownManager,
    private val embeddingManager: NewEmbeddingManager,
    private val hybridDbCreator: HybridDbCreator,
    private val hybridSearchService: HybridSearchService,
) {

    @Async
    fun startCommission(baseID: Long, sourceUrl: String) {
        val commission = CommissionForCreatingDB(
            baseService = baseService,
            appPathsConfig = appPathsConfig,
            normaManager = normManager,
            markDownManager = markdownManager,
            embeddingManager = embeddingManager,
            hybridSearchService = hybridSearchService,
            baseID = baseID,
            sourceUrl = sourceUrl,
            hybridDbCreator = hybridDbCreator,
            commissionStatus = CommissionStatus.EMBEDDED
        )

        //commission.commissionStatus = CommissionStatus.INITIAL
    }
}
