package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController



@RestController
@RequestMapping("/newEmbedding")
class NewEmbeddingManager : PythonServerModel<NewEmbeddingManager.queueElement>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/newEmbedding.py",
    serverName = "newEmbedding",
    autoClose = true
) {

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }


    fun generateEmbeddingsForJson(inputFilePath: String, outputFile: String, commission: CommissionForCreatingDB) {
        val newElement = queueElement(inputFile = inputFilePath, outputFile = outputFile, currentlyHandledOrder = commission)
        this.addToQueue(newElement)
    }


    override fun publishResult(result: String, item: queueElement) {
        item.currentlyHandledOrder.commissionStatus = CommissionStatus.EMBEDDED
        println("Item wassss: $item")
        println("Wynik przetwarzania z serwera $serverName: $result")
    }

    data class queueElement(val inputFile: String,val outputFile: String, val currentlyHandledOrder: CommissionForCreatingDB)
}