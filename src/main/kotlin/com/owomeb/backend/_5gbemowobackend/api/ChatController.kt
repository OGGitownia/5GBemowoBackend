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
class ChatController() {

    @GetMapping("/available-models/{baseId}")
    fun getAvailableModels(@PathVariable baseId: Long): ResponseEntity<List<String>> {
        val result = ChatModel.entries.map { it.name }
        println("Zwracam modele: $result")
        return ResponseEntity.ok(result)
    }

    @GetMapping("/available-tuners/{baseId}")
    fun getAvailableTuners(
        @PathVariable baseId: Long, @RequestParam model: String
    ): ResponseEntity<List<String>> {
        val result = AnswerTuner.entries.map { it.name }
        println("Zwracam tunery: $result")
        return ResponseEntity.ok(result)
    }
}

enum class ChatModel {
    LLaMA_3_8B_Q4_0, LLAMA3_MEDIUM
}

enum class AnswerTuner {
    EXTRA_CONTEXT, CONCISE_STYLE, DOMAIN_OPTIMIZED
}
