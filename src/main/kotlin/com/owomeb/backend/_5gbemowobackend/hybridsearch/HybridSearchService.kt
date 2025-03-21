package com.owomeb.backend._5gbemowobackend.hybridsearch

import com.owomeb.backend._5gbemowobackend.pythonServerModel.PythonServerModel
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.*
import org.json.JSONObject


@Service
@RestController
@RequestMapping("/hybrid_search_server")
class HybridSearchService(
    private val lamoAsker: LamoAsker
) : PythonServerModel<String>(
    scriptPath = "src/main/resources/pythonScripts/hybridsearch/serverSearch.py",
    serverName = "hybrid_search_server",
    autoClose = false
){


    override fun publishResult(result: String, item: String) {

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

        val newQuery = LamoAsker.AugmentedQuery(context = cleanResult, question = item)
        lamoAsker.addToQueue(newQuery)
    }

}
