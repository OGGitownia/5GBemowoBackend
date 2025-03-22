package com.owomeb.backend._5gbemowobackend.appControllers

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.concurrent.ConcurrentHashMap

@Service
class BaseService(
    private val baseRepository: BaseRepository
) {
    // WebSocket tututututututu
    val statusObservers = ConcurrentHashMap<Long, (BaseEntity) -> Unit>()

    fun listAllBases(): List<BaseEntity> {
        val result = baseRepository.findAll()
        println(result)
        return result
    }

    fun getBaseById(id: Long): BaseEntity? {
        return baseRepository.findById(id).orElse(null)
    }

    fun findBySourceUrl(url: String): BaseEntity? {
        return baseRepository.findBySourceUrl(url)
    }

    @Transactional
    fun createBaseIfNotExists(url: String): BaseEntity {
        val existing = baseRepository.findBySourceUrl(url)
        if (existing != null) return existing

        val newBase = BaseEntity(sourceUrl = url)
        return baseRepository.save(newBase)
    }

    @Transactional
    fun updateStatus(baseId: Long, status: BaseStatus, message: String? = null) {
        val base = baseRepository.findById(baseId).orElseThrow()
        base.status = status
        base.statusMessage = message
        baseRepository.save(base)

        //WebSocket tututututu
        statusObservers[baseId]?.invoke(base)
    }

    fun registerStatusObserver(baseId: Long, callback: (BaseEntity) -> Unit) {
        statusObservers[baseId] = callback
    }

    fun unregisterStatusObserver(baseId: Long) {
        statusObservers.remove(baseId)
    }
    @Transactional
    fun delete(base: BaseEntity) {
        println("Próba usuwaniaDELETE")
        baseRepository.delete(base)
    }

}
