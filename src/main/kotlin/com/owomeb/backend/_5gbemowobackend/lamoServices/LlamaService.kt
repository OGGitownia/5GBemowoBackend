package com.owomeb.backend._5gbemowobackend.lamoServices

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.http.*
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import java.util.concurrent.ConcurrentLinkedQueue

@Service
class LlamaService {

    private val apiUrl = "http://127.0.0.1:8081/completion"
    private val restTemplate = RestTemplate()
    private val objectMapper: ObjectMapper = jacksonObjectMapper()

    private val queue: ConcurrentLinkedQueue<Query> = ConcurrentLinkedQueue()

    var ready: Boolean = false
        set(value) {
            println("Zmiana gotowości serwera z $field na $value")
            field = value
            if (value && queue.isNotEmpty()) {
                processQueue()
            }
        }

    fun log(message: String) {
        println("[class LlamaService to co odp::: ] $message")
    }

    data class Query(val question: String, val context: String)

    fun addToQueue(question: String, context: String) {
        queue.add(Query(question, context))
        log("Dodano do kolejki: \"$question\" (kontekst: \"$context\"). Aktualna długość kolejki: ${queue.size}")
    }

    private fun processQueue() {
                val query = queue.poll() ?: return
                log("Przetwarzanie zapytania: \"${query.question}\"")
                val response = askLlama(query.question, query.context)
                log("Odpowiedź: $response")
    }

    fun askLlama(question: String, context: String): String {
        log("Wysyłanie zapytania do Llama 3: \"$question\" (kontekst: \"$context\")")

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val requestBody = objectMapper.writeValueAsString(
            mapOf(
                "prompt" to "Pytanie: $question\nKontekst: $context",
                "n_predict" to 200
            )
        )

        val request = HttpEntity(requestBody, headers)

        return try {
            val response: ResponseEntity<String> = restTemplate.exchange(
                apiUrl, HttpMethod.POST, request, String::class.java
            )
            log("Odpowiedź otrzymana! Treść: ${response.body}")
            response.body ?: "Brak odpowiedzi od modelu."
        } catch (e: Exception) {
            log("Błąd komunikacji z modelem: ${e.message}")
            "Błąd: Nie udało się połączyć z modelem."
        }
    }
}
