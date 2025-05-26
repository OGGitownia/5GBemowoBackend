package com.owomeb.backend._5gbemowobackend.api

import com.fasterxml.jackson.databind.ObjectMapper
import com.owomeb.backend._5gbemowobackend.messageBank.MessageEntity
import com.owomeb.backend._5gbemowobackend.messageBank.MessageRepository
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler
import java.util.concurrent.ConcurrentHashMap

@Component
class MessageSocketHandler(
    private val objectMapper: ObjectMapper,
    private val messageRepository: MessageRepository
) : TextWebSocketHandler() {

    private val sessions = ConcurrentHashMap<String, WebSocketSession>()

    override fun afterConnectionEstablished(session: WebSocketSession) {
        val userId = session.uri?.query?.split("=")?.getOrNull(1)
        if (userId != null) {
            sessions[userId] = session
            val userIdLong = userId.toLong()
            val allMessagesForUser = messageRepository.findAllByUserId(userIdLong)
            allMessagesForUser.forEach { message ->
                sendMessageToUser(userId = userIdLong, message = message)
            }
        }
    }


    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        sessions.entries.removeIf { it.value == session }
    }

    fun sendMessageToUser(userId: Long, message: MessageEntity) {
        println("UserId $userId")
        println("Active WebSocket sessions: ${sessions.keys.joinToString()}")
        println(message.chatId)
        val session = sessions[userId.toString()] ?: return
        val json = objectMapper.writeValueAsString(message)
        session.sendMessage(TextMessage(json))
        println("websocket")
        println("Sent JSON payload: $json")
    }

}