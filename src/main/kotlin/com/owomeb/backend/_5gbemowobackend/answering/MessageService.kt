package com.owomeb.backend._5gbemowobackend.answering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.owomeb.backend._5gbemowobackend.messageBank.MessageEntity
import com.owomeb.backend._5gbemowobackend.messageBank.MessageRepository
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono
import java.util.concurrent.LinkedBlockingQueue

@Service
class MessageService(private val messageRepository: MessageRepository) {

    private val queue = LinkedBlockingQueue<MessageEntity>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val ollamaClient = WebClient.create("http://localhost:11434")

    @PostConstruct
    fun initProcessor() {
        scope.launch {
            ensureOllamaRunning()
            while (true) {
                val message = queue.take()
                println("Processing message: ${message.id} using model ${message.modelName}")
                processMessage(message)
            }
        }
    }

    fun addAnswerToQueue(message: MessageEntity) {
        queue.put(message)
    }

    private suspend fun ensureOllamaRunning() {
        try {
            val process = ProcessBuilder("ollama", "run", "llama3")
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
            println("Ollama process started.")
            delay(3000)
        } catch (e: Exception) {
            println("Failed to start Ollama: ${e.message}")
        }
    }

    private suspend fun processMessage(message: MessageEntity) {
        when (message.modelName) {
            "LLaMA_3_8B_Q4_0" -> queryLlama3(message)
            "LLAMA3_MEDIUM" -> simulateLlama3Medium(message)
            "LLAMA3_SCOUT" -> simulateLlama4Scout(message)
            else -> simulateDefault(message)
        }
    }

    private suspend fun queryLlama3(message: MessageEntity) {
        try {
            val request = mapOf(
                "model" to "llama3",
                "prompt" to message.question,
                "stream" to false
            )

            val response = ollamaClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OllamaResponse::class.java)
                .awaitSingle()

            saveAnswer(message, response.response)
            ///
            println(response)
        } catch (e: Exception) {
            println("Failed to query Ollama: ${e.message}")
            saveAnswer(message, "Error generating answer: ${e.message}")
        }
    }

    private suspend fun simulateLlama3Medium(message: MessageEntity) {
        delay(3000)
        saveAnswer(message, "This is a simulated answer from LLaMA 3 medium")
    }

    private suspend fun simulateLlama4Scout(message: MessageEntity) {
        delay(2500)
        saveAnswer(message, "This is a simulated answer from GPT-4 Turbo.")
    }

    private suspend fun simulateDefault(message: MessageEntity) {
        delay(1000)
        saveAnswer(message, "This is a generic fallback answer.")
    }

    private fun saveAnswer(message: MessageEntity, answer: String) {
        val updated = message.copy(
            answer = answer,
            answered = true,
            answeredAt = java.time.Instant.now()
        )
        messageRepository.save(updated)
        println("Saved answer for message ${updated.id}")
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OllamaResponse(
        @JsonProperty("response") val response: String,
        @JsonProperty("done") val done: Boolean
    )
}
