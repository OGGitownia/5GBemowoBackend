package com.owomeb.backend._5gbemowobackend.api

import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import jakarta.annotation.PreDestroy
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import java.util.concurrent.ConcurrentHashMap

@Component
class BaseStatusWebSocketHandler(
    private val baseService: BaseService
) : TextWebSocketHandler() {

    private val sessionsPerBase: MutableMap<Long, MutableList<WebSocketSession>> = ConcurrentHashMap()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val baseId = extractBaseId(session)
        if (baseId != null) {
            sessionsPerBase.computeIfAbsent(baseId) { mutableListOf() }.add(session)

            // Zarejestruj callback informujący o zmianie statusu
            baseService.registerStatusObserver(baseId) { updatedBase ->
                val json = """
                    {
                      "type": "status-update",
                      "baseId": ${updatedBase.id},
                      "status": "${updatedBase.status}",
                      "message": "${updatedBase.statusMessage ?: ""}"
                    }
                """.trimIndent()
                sessionsPerBase[baseId]?.forEach {
                    try {
                        if (it.isOpen) it.sendMessage(TextMessage(json))
                    } catch (ex: Exception) {
                        println("Błąd wysyłania statusu do klienta: ${ex.message}")
                    }
                }
            }
        } else {
            session.close(CloseStatus.BAD_DATA)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val baseId = extractBaseId(session)
        sessionsPerBase[baseId]?.remove(session)
        if (sessionsPerBase[baseId]?.isEmpty() == true) {
            sessionsPerBase.remove(baseId)
            baseService.unregisterStatusObserver(baseId!!)
        }
    }

    private fun extractBaseId(session: WebSocketSession): Long? {
        val query = session.uri?.query ?: return null
        val idParam = query.split("&").firstOrNull { it.startsWith("baseId=") } ?: return null
        return idParam.removePrefix("baseId=").toLongOrNull()
    }

    @PreDestroy
    fun cleanup() {
        sessionsPerBase.clear()
    }
}

@Component
@EnableWebSocket
class BaseWebSocketConfig(
    private val handler: BaseStatusWebSocketHandler
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/base-status").setAllowedOrigins("*")
    }
}
