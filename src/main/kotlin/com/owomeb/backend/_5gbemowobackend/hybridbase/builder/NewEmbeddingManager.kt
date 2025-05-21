package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.ConcurrentHashMap

@RestController
@RequestMapping("/newEmbedding")
class NewEmbeddingManager : PythonServerModel<NewEmbeddingManager.QueueElement>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/newEmbedding.py",
    serverName = "newEmbedding",
    autoClose = true
) {

    private val callbacks = ConcurrentHashMap<QueueElement, () -> Unit>()

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun generateEmbeddingsForJson(inputFilePath: String, outputFile: String, onFinish: () -> Unit) {
        val element = QueueElement(inputFile = inputFilePath, outputFile = outputFile)
        callbacks[element] = onFinish
        this.addToQueue(element)
    }

    override fun publishResult(result: String, item: QueueElement) {
        println("Zakończono embedding dla: ${item.inputFile}")
        callbacks.remove(item)?.invoke()
        println("Wynik przetwarzania z serwera $serverName: $result")
    }

    data class QueueElement(val inputFile: String, val outputFile: String)
}
