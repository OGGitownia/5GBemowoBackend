package com.owomeb.backend._5gbemowobackend.hybridsearch

import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*

@Service
@RestController
@RequestMapping("/lamo_asker")
class LamoAsker : PythonServerModel<LamoAsker.AugmentedQuery>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/lamoAsker.py",
    serverName = "lamo_asker",
    autoClose = false
) {
    fun ask(context: String, question: String) {
        val augmentedQuery = AugmentedQuery(context, question)
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


    data class AugmentedQuery(val context: String, val question: String)
}
