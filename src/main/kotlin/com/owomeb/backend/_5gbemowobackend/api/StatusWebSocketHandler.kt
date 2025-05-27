package com.owomeb.backend._5gbemowobackend.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class StatusWebSocketHandler(
    private val baseService: BaseService
) : TextWebSocketHandler() {

    private val sessions = ConcurrentHashMap<WebSocketSession, Long>()
    private val mapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val baseIdParam = session.uri?.query?.split("=")?.getOrNull(1)
        val baseId = baseIdParam?.toLongOrNull()

        if (baseId != null) {
            sessions[session] = baseId

            baseService.registerStatusObserver(baseId) { updatedBase ->
                if (session.isOpen) {
                    val json = mapper.writeValueAsString(mapOf(
                        "type" to "statusUpdate",
                        "baseId" to baseId,
                        "status" to updatedBase.status,
                        "message" to updatedBase.statusMessage
                    ))
                    session.sendMessage(TextMessage(json))
                }
            }
        } else {
            session.close(CloseStatus.BAD_DATA)
        }
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val baseId = sessions.remove(session)
        if (baseId != null) {
            baseService.unregisterStatusObserver(baseId)
        }
    }

    override fun handleTransportError(session: WebSocketSession, exception: Throwable) {
        session.close(CloseStatus.SERVER_ERROR)
    }
}