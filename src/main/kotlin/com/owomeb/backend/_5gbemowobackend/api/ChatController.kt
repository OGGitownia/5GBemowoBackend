package com.owomeb.backend._5gbemowobackend.api


import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

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
    LLaMA_3_8B_Q4_0,
    CHAT_GPT4_1,
    GEMINI_2_5_PRO
}


enum class AnswerTuner {

}
