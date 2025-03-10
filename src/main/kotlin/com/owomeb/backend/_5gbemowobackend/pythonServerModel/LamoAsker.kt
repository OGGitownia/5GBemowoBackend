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

    data class AugmentedQuery(val context: String, val question: String)
}
