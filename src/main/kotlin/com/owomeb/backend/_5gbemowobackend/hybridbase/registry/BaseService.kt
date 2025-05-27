package com.owomeb.backend._5gbemowobackend.hybridbase.registry

import com.owomeb.backend._5gbemowobackend.hybridbase.newbuilder.CommissionManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Service
class BaseService(
    private val baseRepository: BaseRepository,
) {
    val statusObservers = ConcurrentHashMap<Long, (BaseEntity) -> Unit>()

    fun listAllBases(): List<BaseEntity> = baseRepository.findAll()

    fun getBaseById(id: Long): BaseEntity? = baseRepository.findById(id).orElse(null)

    fun findBySourceUrl(url: String): BaseEntity? = baseRepository.findBySourceUrl(url)

    @Transactional
    fun createBase(sourceUrl: String, method: String, userId: Long): Long {
        val base = BaseEntity(
            sourceUrl = sourceUrl,
            status = BaseStatus.PENDING,
            createdWthMethod = BaseCreatingMethods.valueOf(method),
            multiSearchAllowed = false,
            maxContextWindow = 4096
        )
        return baseRepository.save(base).id
    }


    @Transactional
    fun updateStatus(baseId: Long, status: BaseStatus, message: String? = null) {
        val base = baseRepository.findById(baseId).orElseThrow()
        base.status = status
        base.statusMessage = message
        baseRepository.save(base)

        statusObservers[baseId]?.invoke(base) // co robi ta linijka
    }

    fun registerStatusObserver(baseId: Long, callback: (BaseEntity) -> Unit) {
        statusObservers[baseId] = callback
    }

    fun unregisterStatusObserver(baseId: Long) {
        statusObservers.remove(baseId)
    }

    @Transactional
    fun delete(base: BaseEntity) {
        println("Próba usuwania bazy ${base.sourceUrl}")
        baseRepository.delete(base)
    }
}
