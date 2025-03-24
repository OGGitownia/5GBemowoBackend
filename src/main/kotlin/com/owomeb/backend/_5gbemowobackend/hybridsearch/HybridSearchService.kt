package com.owomeb.backend._5gbemowobackend.hybridsearch

import com.owomeb.backend._5gbemowobackend.chat.Question
import com.owomeb.backend._5gbemowobackend.chat.QuestionStatus
import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.json.JSONObject

@RestController
@RequestMapping("/hybrid_search_server")
class HybridSearchService: PythonServerModel<HybridSearchService.queueItem>(
    scriptPath = "src/main/resources/pythonScripts/hybridsearch/serverSearch.py",
    serverName = "hybrid_search_server",
    autoClose = false
){

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun search(query: String, question: Question, basePath: String){
        val newSearchObject = queueItem(query, question, basePath)
        //newSearchObject.question.setPathByURL(newSearchObject.question.ur)
        this.addToQueue(newSearchObject)
    }


    override fun publishResult(result: String, item: queueItem) {

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
        item.question.questionStatus = QuestionStatus.WITH_GATHERED_CONTEXT

        //val newQuery = LamoAsker.AugmentedQuery(context = cleanResult, question = item)
        //lamoAsker.addToQueue(newQuery)
    }

    data class queueItem(val query: String, val question: Question, val basePath: String)

}
