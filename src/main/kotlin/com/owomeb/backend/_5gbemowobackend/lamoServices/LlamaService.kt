package com.owomeb.backend._5gbemowobackend.lamoServices

import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentLinkedQueue


/*
class LlamaService(private val llamaRunner: LlamaStartupRunner): PythonServerModel {


    private val queue: ConcurrentLinkedQueue<Query> = ConcurrentLinkedQueue()

    var ready: Boolean = false
        set(value) {
            println("Zmiana gotowości serwera z $field na $value")
            field = value
            if (value && queue.isNotEmpty()) {
                processQueue()
            }
        }

    data class Query(val question: String, val context: String)

    fun addToQueue(question: String, context: String) {
        queue.add(Query(question, context))
        println("Dodano do kolejki: \"$question\" Aktualna kolejka: ${queue.size}")
        if (ready) processQueue()
    }

    private fun processQueue() {
        val query = queue.poll() ?: return
        println("Przetwarzanie zapytania: \"${query.question}\"")
        val response = askLlama(query.question, query.context)
        println("Odpowiedź: $response")
    }

    fun askLlama(question: String, context: String): String {
        println("Wysyłanie zapytania do Llama 3: \"$question\"")

        val formattedPrompt = """
        Pytanie: $question
        Kontekst:${context.split("\n").joinToString("\n- ") { it.trim() }}
        Ważne: Odpowiedz **tylko** na podstawie dostarczonego kontekstu. 
        **Jeśli kontekst nie zawiera odpowiednich informacji, napisz: "Brak informacji w kontekście".**""".trimIndent()

        return try {
            llamaRunner.generateResponse(formattedPrompt)
        } catch (e: Exception) {
            println("Błąd komunikacji z modelem: ${e.message}" + e)
            "Błąd: Nie udało się połączyć z modelem"
        }
    }
}

 */
