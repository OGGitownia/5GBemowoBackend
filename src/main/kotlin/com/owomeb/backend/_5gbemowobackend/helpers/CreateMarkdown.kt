package com.owomeb.backend._5gbemowobackend.helpers

import com.owomeb.backend._5gbemowobackend.core.AppPathsConfig
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.FinalMarkdown


fun main(){
        println("Hello World!")
        val finalMarkdown = FinalMarkdown()
        val appPathsConfig = AppPathsConfig()
        finalMarkdown.doMarkdowning(
            inputPath = appPathsConfig.getExtractedDocx("63"),
            outputPath = appPathsConfig.getMarkdownPath("63")
        )
}
