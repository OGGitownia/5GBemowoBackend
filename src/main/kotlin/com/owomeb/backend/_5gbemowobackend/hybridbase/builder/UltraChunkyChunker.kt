package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class UltraChunkyChunker(
    private val pureMarkdownPath: String,
    private val outputPath: String,
    private val minChunkLen: Int = 128,
    private val maxChunkLen: Int = 4048
) {

    fun process() {
        val rawChunks = splitByChapters(File(pureMarkdownPath).readText())

        val processedChunks = rawChunks
            .flatMap { chunk ->
                if (chunk.length > maxChunkLen) splitByEmptyLines(chunk) else listOf(chunk)
            }
            .filter { it.length >= minChunkLen } // usuwanie zbyt krótkich chunków

        println("Krok 1 - podział na rozdziały + podział długich chunków + filtracja")
        analyseChunks(processedChunks)
        saveChunksToJsonFile(processedChunks)
    }

    private fun splitByChapters(text: String): List<String> {
        return text.split(Regex("^### ", RegexOption.MULTILINE))
            .mapIndexed { index, value ->
                if (index == 0) value.trim()
                else "### ${value.trim()}"
            }.filter { it.isNotBlank() }
    }

    private fun splitByEmptyLines(chunk: String): List<String> {
        val sections = chunk.split(Regex("(\\n\\s*\\n)+"))
        val result = mutableListOf<String>()
        val current = StringBuilder()

        for (section in sections) {
            if (current.length + section.length + 2 > maxChunkLen && current.isNotEmpty()) {
                result.add(current.toString().trim())
                current.clear()
            }
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(section.trim())
        }

        if (current.isNotEmpty()) {
            result.add(current.toString().trim())
        }

        return result
    }

    private fun analyseChunks(chunks: List<String>) {
        val lengths = chunks.map { it.length }
        val tooShort = lengths.count { it < minChunkLen }
        val tooLong = lengths.count { it > maxChunkLen }
        val avg = if (lengths.isNotEmpty()) lengths.sum() / lengths.size else 0
        println("Chunki: ${chunks.size}, Zbyt krótkie: $tooShort, Zbyt długie: $tooLong, Najkrótszy: ${lengths.minOrNull()}, Najdłuższy: ${lengths.maxOrNull()}, Średnia: $avg")
    }

    private fun saveChunksToJsonFile(chunks: List<String>) {
        val outputFile = File(outputPath)
        outputFile.parentFile.mkdirs()

        val jsonObject = ChunkJsonWrapper(chunks)
        val json = Json { prettyPrint = true }.encodeToString(jsonObject)
        outputFile.writeText(json)

        println("Zapisano ${chunks.size} chunków w formacie JSON do: ${outputFile.absolutePath}")
    }

    @Serializable
    data class ChunkJsonWrapper(val chunks: List<String>)
}
