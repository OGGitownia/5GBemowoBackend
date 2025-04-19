package com.owomeb.backend._5gbemowobackend.answering

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.api.ChatWebSocketSender
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseRepository
import com.owomeb.backend._5gbemowobackend.hybridbase.retrieval.HybridSearchService
import java.io.File
import java.time.LocalDateTime


class Question(private val hybridSearchService: HybridSearchService,
               private val baseRepository: BaseRepository,
               private val appPathsConfig: AppPathsConfig,
               private val lamoAsker: LamoAsker,
               private val chatWebSocketSender: ChatWebSocketSender,
               questionStatus: QuestionStatus = QuestionStatus.ADOPTED,
               private val question: String,
               private val baseURL: String){

    private var basePath: String

    var context = ""


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
                    QuestionStatus.ANSWERED_AND_DELIVERED -> print("tyle")
                }
            }
        }


    private fun addToAnserQuestionHistory(path: String) {
        val file = File(path)

        if (!file.exists()) {
            println("Creating new answer history file: $path")
            file.parentFile.mkdirs()
            file.writeText("=== HISTORY OF GENERATED ANSWERS ===\n\n")
        }

        val dateTime = LocalDateTime.now().toString().replace("T", " ")

        val processedQuestion = question.trim()
        val processedContext = context.trim()
        val processedAnswer = answer.trim()

        val entry = buildString {
            append(">>> Question: $processedQuestion\n")
            append("Date: $dateTime\n\n")
            append("Context used:\n")
            append("$processedContext\n\n")
            append("Generated answer:\n")
            append("$processedAnswer\n")
            append("\n")
            append("-".repeat(80))
            append("\n\n")
        }

        file.appendText(entry)
        println("Added question to TXT history at: $path")
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
        addToAnserQuestionHistory(appPathsConfig.getHistoryPath())
    }

    private fun answer() {
        println("Question: $question")
        println("Context: $context")
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

