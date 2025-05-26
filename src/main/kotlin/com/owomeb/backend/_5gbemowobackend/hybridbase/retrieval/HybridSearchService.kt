package com.owomeb.backend._5gbemowobackend.hybridbase.retrieval

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.*
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

@RestController
@RequestMapping("/hybrid_search_server")
class HybridSearchService: PythonServerModel<HybridSearchService.QueueItem>(
    scriptPath = "src/main/resources/pythonScripts/pythonServers/serverSearch.py",
    serverName = "hybrid_search_server",
    autoClose = false
){

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun search(query: String, basePath: String, onFinish: (String) -> Unit){
        val newSearchObject = QueueItem(
            query = query,
            basePath = basePath,
            onFinish = onFinish
        )
        this.addToQueue(newSearchObject)
    }


    override fun publishResult(result: String, item: QueueItem) {

        val cleanResult = try {
            val json = JSONObject(result)
            val resultsArray = json.optJSONArray("results")

            if (resultsArray != null) {
                (0 until resultsArray.length()).joinToString("\n") { index ->
                    resultsArray.optString(index)
                }
            } else {
                "Brak wyników"
            }
        } catch (e: Exception) {
            "Błąd parsowania JSON: ${e.message}"
        }

        println("Result of hybrid search for: $item  is  \n $cleanResult")
        item.onFinish(cleanResult)
    }

    override fun sendRequestToPython(item: QueueItem, callback: (String) -> Unit) {
        thread {
            try {
                val url = URL("http://localhost:$actualPort/$serverName/process")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")

                val objectMapper = jacksonObjectMapper()
                val requestBody = objectMapper.writeValueAsString(
                    mapOf(
                        "query" to item.query,
                        "basePath" to item.basePath
                    )
                )

                connection.outputStream.use {
                    it.write(requestBody.toByteArray(Charsets.UTF_8))
                }

                val response = connection.inputStream.bufferedReader().use { it.readText() }

                if (connection.responseCode == 200) {
                    callback(response)
                } else {
                    println("Błąd: $response")
                }
            } catch (e: Exception) {
                println("Błąd komunikacji: ${e.message}")
            }
        }
    }


    data class QueueItem(
        val query: String,
        val basePath: String,
        val onFinish: (String) -> Unit
    )

}
