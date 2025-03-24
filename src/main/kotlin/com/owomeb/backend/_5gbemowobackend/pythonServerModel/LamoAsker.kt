package com.owomeb.backend._5gbemowobackend.hybridsearch

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.owomeb.backend._5gbemowobackend.chat.Question
import com.owomeb.backend._5gbemowobackend.chat.QuestionStatus
import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/lamo_asker")
class LamoAsker : PythonServerModel<LamoAsker.AugmentedQuery>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/lamoAsker.py",
    serverName = "lamo_asker",
    autoClose = false
) {

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun ask(context: String, question: String, questionObject: Question) {
        val augmentedQuery = AugmentedQuery(context, question, questionObject)
        this.addToQueue(augmentedQuery)
    }
    fun streamResponse(
        question: String,
        context: String,
        onToken: (String, Boolean) -> Unit
    ) {
        val mockResponse = "This is a simulated answer for testing purposes only."
        val tokens = mockResponse.split(" ")

        for ((i, token) in tokens.withIndex()) {
            onToken(token + " ", i == tokens.lastIndex)
            Thread.sleep(250) // symulacja opóźnienia generacji tokena
        }
    }

    private val mapper = jacksonObjectMapper()

    override fun publishResult(result: String, item: AugmentedQuery) {
        println(result)
        val parsed: Map<String, String> = mapper.readValue(result)
        val responseText = parsed["response"] ?: "Brak odpowiedzi"
        item.questionObject.answer = responseText
        println(responseText)
        item.questionObject.questionStatus = QuestionStatus.ANSWERED
    }


    data class AugmentedQuery(val context: String, val question: String, val questionObject: Question)
}
