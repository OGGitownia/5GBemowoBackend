package com.owomeb.backend._5gbemowobackend.hybridbase.builder

    fun main(){
        println("Hello World!")
        val finalChunker = FinalChunker(
            pureMarkdownPath = "src/main/resources/data/64/markdown.md",
            outputPath = "src/main/resources/data/64/chunks3.json",
            minChunkLen = 64,
            maxChunkLen = 4096
        )
        finalChunker.process()
    }