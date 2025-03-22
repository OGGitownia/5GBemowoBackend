package com.owomeb.backend._5gbemowobackend.appControllers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.owomeb.backend._5gbemowobackend.hybridsearch.LamoAsker
import org.springframework.stereotype.Component
import org.springframework.web.socket.*
import org.springframework.web.socket.handler.TextWebSocketHandler
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

@Component
class QuestionWebSocketHandler(
    private val lamoAsker: LamoAsker
) : TextWebSocketHandler() {

    private val activeSessions = ConcurrentHashMap<String, WebSocketSession>()
    private val generationJobs = ConcurrentHashMap<String, Thread>()
    private val objectMapper = jacksonObjectMapper()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val sessionId = session.id
        activeSessions[sessionId] = session
        println("Nowe połączenie WebSocket: $sessionId")
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        val sessionId = session.id
        generationJobs[sessionId]?.interrupt()
        generationJobs.remove(sessionId)
        activeSessions.remove(sessionId)
        println("Zamknięto połączenie WebSocket: $sessionId")
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        val sessionId = session.id
        val json = message.payload

        val payload = try {
            objectMapper.readValue<SocketRequest>(json)
        } catch (e: Exception) {
            session.sendMessage(TextMessage("""{ "type": "error", "message": "Invalid JSON" }"""))
            return
        }

        when (payload.type) {
            "ask" -> {
                val questionId = UUID.randomUUID().toString()

                val job = thread {
                    try {
                        lamoAsker.streamResponse(payload.question ?: "", payload.context ?: "") { token, isLast ->
                            val msg = mapOf(
                                "type" to "token",
                                "questionId" to questionId,
                                "token" to token,
                                "done" to isLast
                            )
                            if (session.isOpen) {
                                session.sendMessage(TextMessage(objectMapper.writeValueAsString(msg)))
                            }
                        }
                    } catch (e: InterruptedException) {
                        println("Generowanie przerwane dla sesji $sessionId")
                    } catch (e: Exception) {
                        val err = mapOf("type" to "error", "message" to (e.message ?: "Unknown error"))
                        session.sendMessage(TextMessage(objectMapper.writeValueAsString(err)))
                    }
                }

                generationJobs[sessionId] = job
            }

            "cancel" -> {
                generationJobs[sessionId]?.interrupt()
                generationJobs.remove(sessionId)
                val msg = mapOf("type" to "cancelled")
                session.sendMessage(TextMessage(objectMapper.writeValueAsString(msg)))
            }
        }
    }

    data class SocketRequest(
        val type: String,
        val question: String? = null,
        val context: String? = null
    )
}

@Component
class QuestionWebSocketConfig(
    private val handler: QuestionWebSocketHandler
) : WebSocketConfigurer {
    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry.addHandler(handler, "/ws/question").setAllowedOrigins("*")
    }
}
