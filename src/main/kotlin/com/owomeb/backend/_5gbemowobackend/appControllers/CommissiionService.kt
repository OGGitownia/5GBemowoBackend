package com.owomeb.backend._5gbemowobackend.appControllers

import com.owomeb.backend._5gbemowobackend.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.baseCreators.MarkdownManager
import com.owomeb.backend._5gbemowobackend.pythonServerModel.NewEmbeddingManager
import com.owomeb.backend._5gbemowobackend.baseCreators.NormManager
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchManagerController
import com.owomeb.backend._5gbemowobackend.hybridsearch.HybridSearchService
import com.owomeb.backend._5gbemowobackend.pythonServerModel.HybridDbCreator
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
        val commission = Commission(
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
