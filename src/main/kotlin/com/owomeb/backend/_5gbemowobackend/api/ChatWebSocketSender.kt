package com.owomeb.backend._5gbemowobackend.api

import org.springframework.messaging.simp.SimpMessagingTemplate
import org.springframework.stereotype.Component

@Component
class ChatWebSocketSender(
    private val messagingTemplate: SimpMessagingTemplate
) {

    fun sendAnswer(answer: String) {
        println("Wysyłam do /topic/chat: $answer")

        messagingTemplate.convertAndSend("/topic/chat", mapOf("answer" to answer))
    }
}
