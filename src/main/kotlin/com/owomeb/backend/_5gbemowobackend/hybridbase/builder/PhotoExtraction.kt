package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.owomeb.backend._5gbemowobackend.answering.Question
import com.owomeb.backend._5gbemowobackend.answering.QuestionStatus
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.*
import java.nio.file.Paths


@RestController
@RequestMapping("/unikalnanazwa")
class PhotoExtraction : PythonServerModel<PhotoExtraction.AugmentedQuery>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/photoExtraction.py",
    serverName = "unikalnanazwa",
    autoClose = true
) {

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }



    fun extract(input: String, outputDocx: String, outputDir: String) {
        val absoluteInput = Paths.get(input).toAbsolutePath().toString() + ".docx"
        val augmentedQuery = AugmentedQuery(absoluteInput, outputDir, outputDocx)
        this.addToQueue(augmentedQuery)
    }

    override fun publishResult(result: String, item: AugmentedQuery) {
        println("print z metody published result na koniec dzialania ")


    }


    data class AugmentedQuery(val input: String, val outputDocx: String, val outputDir: String)
}
