package com.owomeb.backend._5gbemowobackend.answering

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.owomeb.backend._5gbemowobackend.api.MessageSocketHandler
import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
import com.owomeb.backend._5gbemowobackend.messageBank.MessageEntity
import com.owomeb.backend._5gbemowobackend.messageBank.MessageRepository
import jakarta.annotation.PostConstruct
import kotlinx.coroutines.*
import kotlinx.coroutines.reactive.awaitSingle
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.time.Instant
import java.util.concurrent.LinkedBlockingQueue

@Service
class MessageService(
    private val messageRepository: MessageRepository,
    private val hybridSearchService: HybridSearchService,
    private val appPathsConfig: AppPathsConfig,
    private val messageSocketHandler: MessageSocketHandler
) {

    private val queue = LinkedBlockingQueue<MessageEntity>()
    private val scope = CoroutineScope(Dispatchers.Default)
    private val ollamaClient = WebClient.create("http://localhost:11434")

    private val PROMPT_TEMPLATE = """
    If there is an extract which looks like photo_X.format 
    where X is a number and format is .png or .emf, it is a photo code.

    Place the number of that photo at the end of the answer. 
    Give as specific answers as possible.

    Remember to place the photo code at the bottom exactly in the same format 
    as it is present in the context. And to make it clear, I want to see something 
    like photo_2.emf — not like photo_2.format.

    Photo code should be written in lowercase.

    Answer the question based only on the following context:

    {context}

    ---

    Answer the question: {question}
"""


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
            val process = withContext(Dispatchers.IO) {
                ProcessBuilder("ollama", "run", "llama3")
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectError(ProcessBuilder.Redirect.INHERIT)
                    .start()
            }
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
            hybridSearchService.search(
                query = message.question,
                basePath = appPathsConfig.getHybridBaseDirectory(message.baseId),
                onFinish = { contextForQuery ->

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val prompt = PROMPT_TEMPLATE
                                .replace("{context}", contextForQuery)
                                .replace("{question}", message.question)

                            val request = mapOf(
                                "model" to "llama3",
                                "prompt" to prompt,
                                "stream" to false
                            )

                            val response = ollamaClient.post()
                                .uri("/api/generate")
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(request)
                                .retrieve()
                                .bodyToMono(OllamaResponse::class.java)
                                .awaitSingle()

                            val updatedMessage = saveAnswer(message, response.response)
                            println("response.response")
                            messageSocketHandler.sendMessageToUser(message.userId, updatedMessage)

                        } catch (e: Exception) {
                            println("Ollama failed: ${e.message}")
                            saveAnswer(message, "Error generating answer: ${e.message}")
                        }
                    }
                }

            )
        } catch (e: Exception) {
            println("Hybrid search failed: ${e.message}")
            saveAnswer(message, "Error in hybrid search: ${e.message}")
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

    private fun saveAnswer(message: MessageEntity, answer: String): MessageEntity {
        val updated = message.copy(
            answer = answer,
            answered = true,
            answeredAt = Instant.now()
        )
        messageRepository.save(updated)
        println("Saved answer for message ${updated.id}")
        return updated
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    data class OllamaResponse(
        @JsonProperty("response") val response: String,
        @JsonProperty("done") val done: Boolean
    )
}
