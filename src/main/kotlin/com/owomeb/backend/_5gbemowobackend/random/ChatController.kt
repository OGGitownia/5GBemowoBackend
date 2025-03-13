package com.owomeb.backend._5gbemowobackend.random

import com.owomeb.backend._5gbemowobackend.lamoServices.LlamaService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.beans.factory.annotation.Autowired


/*
@RestController
@RequestMapping("/api/chat")
class ChatController @Autowired constructor(private val llamaService: LlamaService) {

    @PostMapping
    fun chat(@RequestBody payload: Map<String, String>): ResponseEntity<Map<String, String>> {
        val userMessage = payload["message"] ?: return ResponseEntity.badRequest().body(mapOf("reply" to "Błąd: Brak wiadomości"))

        println("Otrzymano wiadomość od użytkownika: \"$userMessage\"")

        val botReply = llamaService.addToQueue(userMessage, "") // Wysyłamy pytanie do Llama 3
        println(" Odpowiedź od Llama: \"$botReply\"")

        val response = mapOf("reply" to botReply)
        return ResponseEntity.ok(response)
    }
}

 */
