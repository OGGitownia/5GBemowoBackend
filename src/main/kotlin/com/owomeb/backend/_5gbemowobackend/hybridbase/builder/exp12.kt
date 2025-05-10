package com.owomeb.backend._5gbemowobackend.hybridbase.builder

    fun main(){
        println("Hello World!")
        val finalChunker = FinalChunker(
            pureMarkdownPath = "src/main/resources/data/64/markdown.md",
            outputPath = "src/main/resources/data/64/chunks5.json",
        )
        finalChunker.process()
    }