package com.owomeb.backend._5gbemowobackend

import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod

@Service
class LlamaService {

    private val apiUrl = "http://127.0.0.1:8081/completion"
    private val restTemplate = RestTemplate()

    fun log(message: String) {
        println("[LlamaService] $message")
    }

    fun askLlama(question: String): String {
        log("Wysyłanie zapytania do Llama 3: \"$question\"")

        val headers = HttpHeaders()
        headers.set("Content-Type", "application/json")

        val requestBody = """
            {
                "prompt": "$question",
                "n_predict": 200
            }
        """.trimIndent()

        val request = HttpEntity(requestBody, headers)

        return try {
            val response: ResponseEntity<String> = restTemplate.exchange(apiUrl, HttpMethod.POST, request, String::class.java)
            log("✅ Odpowiedź otrzymana!")
            response.body ?: "Brak odpowiedzi od modelu."
        } catch (e: Exception) {
            log("❌ Błąd komunikacji z modelem: ${e.message}")
            "Błąd: Nie udało się połączyć z modelem."
        }
    }
}
