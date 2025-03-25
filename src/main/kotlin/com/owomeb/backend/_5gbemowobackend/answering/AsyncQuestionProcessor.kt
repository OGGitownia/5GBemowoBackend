package com.owomeb.backend._5gbemowobackend.answering

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.api.ChatWebSocketSender
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseRepository
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService


class Question(private val hybridSearchService: HybridSearchService,
               private val baseRepository: BaseRepository,
               private val appPathsConfig: AppPathsConfig,
               private val lamoAsker: LamoAsker,
               private val chatWebSocketSender: ChatWebSocketSender,
               questionStatus: QuestionStatus = QuestionStatus.ADOPTED,
               private val question: String,
               private val baseURL: String){

    private var basePath: String

    private var context = ""


    var answer = ""
    init {
        basePath = setPathByURL(baseURL)
        gatherContext()
    }



    private fun setPathByURL(baseURL: String): String {
        val base = baseRepository.findBySourceUrl(baseURL)
        if (base != null) {
            print(base.id.toString())
            return appPathsConfig.getHybridBaseDirectory(base.id.toString())
        }else{
            println("Error")
            println(this.baseURL)
        }
        return ""
    }

    var questionStatus: QuestionStatus = questionStatus
        set(value) {
            if (field != value) {
                println("Question status changed from $field to $value")
                field = value

                when (value) {
                    QuestionStatus.ADOPTED -> {}
                    QuestionStatus.WITH_GATHERED_CONTEXT -> answer()
                    QuestionStatus.WHILE_BEING_ANSWERED -> {}
                    QuestionStatus.ANSWERED -> deliver()
                    QuestionStatus.ANSWERED_AND_DELIVERED -> TODO()
                }
            }
        }

    private fun gatherContext() {
        hybridSearchService.search(
            question = this,
            query = question,
            basePath = basePath,
        )
    }


    private fun deliver() {
        println("deliver$answer")
        chatWebSocketSender.sendAnswer(answer)
    }

    private fun answer() {
        lamoAsker.ask(
            context = context,
            question = question,
            questionObject = this
        )
    }

}


enum class QuestionStatus {
    ADOPTED,
    WITH_GATHERED_CONTEXT,
    WHILE_BEING_ANSWERED,
    ANSWERED,
    ANSWERED_AND_DELIVERED
}

