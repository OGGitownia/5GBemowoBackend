package com.owomeb.backend._5gbemowobackend.api

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WebSocketConfig(
    private val messageSocketHandler: MessageSocketHandler
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        registry
            .addHandler(messageSocketHandler, "/ws/messages")
            .setAllowedOrigins("*") // dla testów – później ogranicz do domeny
    }
}
