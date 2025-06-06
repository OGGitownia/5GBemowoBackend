package com.owomeb.backend._5gbemowobackend.core

import org.springframework.stereotype.Component

@Component
class AppPathsConfig {

    private val normsDirectory: String = "src/main/resources/data"

    fun getNormDirectory(normName: String): String {
        return "$normsDirectory/$normName"
    }

    fun getMarkdownPath(normName: String): String {
        return "${getNormDirectory(normName)}/markdown.md"
    }

    fun getPureMarkdownPath(normName: String): String {
        return "${getNormDirectory(normName)}/markdown_pure.md"
    }

    fun getZipPath(normName: String): String {
        return "${getNormDirectory(normName)}/originalZIP.zip"
    }

    fun getDocPath(normName: String): String {
        return "${getNormDirectory(normName)}/norm"
    }
    fun getExtractedDocx(normName: String): String {
        return "${getNormDirectory(normName)}/extractedNorm"
    }

    fun getEmbeddedJsonPath(normName: String): String {
        return "${getNormDirectory(normName)}/embedded_chunks.json"
    }

    fun getChunkedJsonPath(normName: String): String {
        return "${getNormDirectory(normName)}/chunky.json"
    }
    fun getHybridBaseDirectory(normName: String): String {
        return "${getNormDirectory(normName)}/hybrid_base"
    }
    fun getHistoryPath(): String {
        return "src/main/resources/history/historyOneOne.txt"
    }
    fun getPhotoExtractedDocx(normName: String): String{
        return "${getNormDirectory(normName)}/normAsDocxWithAfterExtraction.docx"
    }


}
