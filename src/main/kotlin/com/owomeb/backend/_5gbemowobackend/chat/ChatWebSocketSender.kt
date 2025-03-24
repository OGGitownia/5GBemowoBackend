package com.owomeb.backend._5gbemowobackend.chat

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class ChatWebSocketSender(
    private val messagingTemplate: SimpMessagingTemplate
) {
    private val mapper = jacksonObjectMapper()

    fun sendAnswer(answer: String) {
        println("Wysyłam do /topic/chat: $answer")

        messagingTemplate.convertAndSend("/topic/chat", mapOf("answer" to answer))
    }
}
