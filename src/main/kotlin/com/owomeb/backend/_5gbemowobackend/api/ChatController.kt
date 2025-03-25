package com.owomeb.backend._5gbemowobackend.api

import com.owomeb.backend._5gbemowobackend.answering.LamoAsker
import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.answering.Question
import com.owomeb.backend._5gbemowobackend.answering.QuestionStatus
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseRepository
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus

@RestController
@RequestMapping("/api/chat")
class ChatController(
    private val hybridSearchService: HybridSearchService,
    private val baseRepository: BaseRepository,
    private val appPathsConfig: AppPathsConfig,
    private val lamoAsker: LamoAsker,
    private val chatWebSocketSender: ChatWebSocketSender
){

    data class ChatRequest(
        val baseName: String,
        val question: String
    )

    @PostMapping("/ask")
    fun handleQuestion(@RequestBody request: ChatRequest): ResponseEntity<String> {

        val question = Question(
            question = request.question,
            hybridSearchService = hybridSearchService,
            questionStatus = QuestionStatus.ADOPTED,
            baseRepository = baseRepository,
            appPathsConfig = appPathsConfig,
            baseURL = request.baseName,
            lamoAsker = lamoAsker,
            chatWebSocketSender = chatWebSocketSender
        )

        return ResponseEntity("Zapytanie zostało przesłane do serwera hybrydowego", HttpStatus.ACCEPTED)
    }
}
