package com.owomeb.backend._5gbemowobackend.messageBank

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.owomeb.backend._5gbemowobackend.api.MessageSocketHandler
import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
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

    private val SYSTEM_PROMPT = """
        You are a technical assistant specialized in 3GPP standards, including specifications such as LTE, NR, and 5G Core. You must provide highly detailed, precise, and technically accurate answers to questions based only on the provided context.

If the context contains references to images in the following format:
'photo_X.extension : Figure identificator: photo_name'
(e.g., 'photo_24.emf : Figure 5.3.3.1-1: RRC connection establishment, successful'),
treat these as embedded figure references.

Whenever a figure reference appears in the context and the **photo_name** is directly relevant to the question (e.g., the procedure described in the figure matches the one being asked about), include the **entire figure reference**, consisting of:
- the filename (e.g., photo_24.emf), and
- the figure identifier and title (e.g., Figure 5.3.3.1-1: RRC connection establishment, successful)

Insert this complete figure reference in a **logically appropriate place** outside of the sentence, ideally at the end of the sentence that most directly relates to the figure. Example:  
"This procedure is illustrated in photo_24.emf : Figure 5.3.3.1-1: RRC connection establishment, successful."

If the user’s question refers to a term or phrase that appears in a figure title (photo_name), assume the figure is relevant. Always include the full figure reference, including the filename and figure identifier, at a relevant place in the answer, even if the user does not explicitly mention a figure.

Do **not invent or modify figure references**, and do **not include figures that are unrelated** to the answer.

You must not answer questions that cannot be answered using the context. Do not hallucinate facts, behaviors, or diagram names that are not explicitly given.

        """.trimIndent()

    private val PROMPT_TEMPLATE = """
Answer the following question using only the provided context. Include exact figure references if the figure supports your answer.
Context:
{context}

    ---

    Answer the question: {question}
    """.trimIndent()


    @PostConstruct
    fun initProcessor() {
        scope.launch {
            ensureOllamaRunning()
            while (true) {
                val message = queue.take()
                println("Processing message: ${message.id} using model ${message.modelName}")
                println(message.question)
                processMessage(message)
            }
        }
    }

    fun addQuestionToQueue(message: MessageEntity) {
        queue.put(message)
    }

    private suspend fun ensureOllamaRunning() {
        try {
            val process = withContext(Dispatchers.IO) {
                ProcessBuilder("ollama", "run", "llama2:13b")
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
                                "model" to "llama2:13b",
                                "system" to SYSTEM_PROMPT,
                                "prompt" to prompt,
                                "stream" to false
                            )
                            println("Piastow")
                            println(prompt)

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
        try {
            hybridSearchService.search(
                query = message.question,
                basePath = appPathsConfig.getHybridBaseDirectory(message.baseId),
                onFinish = { contextForQuery ->

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val openAiApiKey = System.getenv("OPENAI_API_KEY")  // lub wstrzykiwane z konfiguracji
                            val openAiClient = WebClient.builder()
                                .baseUrl("https://api.openai.com/v1")
                                .defaultHeader("Authorization", "Bearer $openAiApiKey")
                                .defaultHeader("Content-Type", "application/json")
                                .build()

                            val systemPrompt = """
                            You are a helpful assistant. 
                            Answer the question strictly based on the provided context.
                            If you see any reference like photo_X.Y (e.g., photo_2.emf), place it on a separate line at the end of the answer.
                            Never answer from outside the context, and if you’re unsure, say so.
                        """.trimIndent()

                            val userContextMessage = """
                            Context:
                            $contextForQuery
                        """.trimIndent()

                            val requestBody = mapOf(
                                "model" to "gpt-4",  // lub "gpt-3.5-turbo"
                                "messages" to listOf(
                                    mapOf("role" to "system", "content" to systemPrompt),
                                    mapOf("role" to "user", "content" to userContextMessage),
                                    mapOf("role" to "user", "content" to message.question)
                                ),
                                "stream" to false
                            )

                            val response = openAiClient.post()
                                .uri("/chat/completions")
                                .bodyValue(requestBody)
                                .retrieve()
                                .bodyToMono(OpenAiResponse::class.java)
                                .awaitSingle()

                            val answer = response.choices.firstOrNull()?.message?.content ?: "No answer generated."
                            val updatedMessage = saveAnswer(message, answer)
                            messageSocketHandler.sendMessageToUser(message.userId, updatedMessage)

                        } catch (e: Exception) {
                            println("OpenAI GPT API failed: ${e.message}")
                            saveAnswer(message, "Error while calling GPT: ${e.message}")
                        }
                    }
                }
            )
        } catch (e: Exception) {
            println("Hybrid search failed: ${e.message}")
            saveAnswer(message, "Error in hybrid search: ${e.message}")
        }
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
