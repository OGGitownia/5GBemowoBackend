package com.owomeb.backend._5gbemowobackend.baseCreators

import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.apache.poi.ss.formula.functions.T
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@Service
@RestController
@RequestMapping("/newEmbedding")
class NewEmbeddingManager : PythonServerModel<NewEmbeddingManager.queueElement>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/newEmbedding.py",
    serverName = "newEmbedding",
    autoClose = true
) {

    fun generateEmbeddingsForJson(inputFilePath: String, outputFile: String) {
        val newElement = queueElement(inputFile = inputFilePath, outputFile = outputFile)
        this.addToQueue(newElement)
    }

    @Override
    fun publishResult(result: String, item: T) {
        println("Item wassss: $item")
        println("Wynik przetwarzania z serwera $serverName: $result")
    }

    data class queueElement(val inputFile: String,val outputFile: String) {}
}