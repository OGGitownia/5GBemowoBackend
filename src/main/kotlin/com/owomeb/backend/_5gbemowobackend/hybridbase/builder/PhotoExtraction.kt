package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.file.Paths
import java.util.concurrent.ConcurrentHashMap

// /app/src/main/resources/pythonScripts/pythonServers/photoExtraction.py

@RestController
@RequestMapping("/photoExtraction")
class PhotoExtraction : PythonServerModel<PhotoExtraction.AugmentedQuery>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/photoExtraction.py",
    serverName = "photoExtraction",
    autoClose = true
) {

    private val callbacks = ConcurrentHashMap<AugmentedQuery, () -> Unit>()

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun extract(input: String, outputDocx: String, outputDir: String, onFinish: () -> Unit) {
        val absoluteInput = Paths.get(input).toAbsolutePath().toString()
        val query = AugmentedQuery(absoluteInput, outputDocx, outputDir)
        callbacks[query] = onFinish
        this.addToQueue(query)
    }

    override fun publishResult(result: String, item: AugmentedQuery) {
        println("Zakończono extract() dla: ${item.input}")
        callbacks.remove(item)?.invoke()
    }

    data class AugmentedQuery(val input: String, val outputDocx: String, val outputDir: String)
}
