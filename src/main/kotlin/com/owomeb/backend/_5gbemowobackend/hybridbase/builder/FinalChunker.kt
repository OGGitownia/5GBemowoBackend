package com.owomeb.backend._5gbemowobackend.hybridbase.builder


import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class FinalChunker(
    private val pureMarkdownPath: String,
    private val outputPath: String,
    private val minChunkLen: Int = 128,
    private val maxChunkLen: Int = 4048
) {

    fun splitByHeader(level: Int, chunks: List<String>, minChunkLen: Int, maxChunkLen: Int): List<String> {
        println()
        val pattern = "^${"#".repeat(level)} ".toRegex(RegexOption.MULTILINE)

        val tooLongBefore = chunks.count { it.length > maxChunkLen }
        println("Za długie chunki przed podziałem $pattern: $tooLongBefore")

        val result = mutableListOf<String>()

        for (chunk in chunks) {
            if (chunk.length < minChunkLen && result.isNotEmpty()) {
                // 🔗 Łączymy za krótkie, jeśli to możliwe
                val lastChunk = result.last()
                if ((lastChunk.length + chunk.length) <= maxChunkLen) {
                    result[result.size - 1] = "$lastChunk\n\n$chunk".trim()
                } else {
                    result.add(chunk)
                }
            } else if (chunk.length > maxChunkLen) {
                val splitChunks = chunk.split(pattern)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val cleanedChunks = splitChunks.mapIndexed { index, part ->
                    if (index == 0) part // pierwszy fragment nie ma nagłówka
                    else part.substringAfter("\n").trim() // reszta powinna mieć usunięty nagłówek
                }

                println("Dzielimy chunk na ${cleanedChunks.size} części")
                result.addAll(cleanedChunks)
            } else {
                result.add(chunk)
            }
        }

        val tooLongAfter = result.count { it.length > maxChunkLen }
        println("Za długie chunki po podziale: $tooLongAfter")
        println("Różnica po podziale $pattern: ${tooLongAfter - tooLongBefore}")
        println()

        return if (tooLongAfter > 0 && tooLongAfter != tooLongBefore) {
            splitByHeader(level + 1, result, minChunkLen, maxChunkLen)
        } else {
            result
        }
    }

    fun splitByNewline(chunks: List<String>, minChunkLen: Int, maxChunkLen: Int): List<String> {
        println()
        println("Dzielenie po znaku nowej linii najbliżej środka")

        val tooLongBefore = chunks.count { it.length > maxChunkLen }
        println("Za długie chunki przed podziałem (newline): $tooLongBefore")

        val result = mutableListOf<String>()

        for (chunk in chunks) {

            if (chunk.contains("</tr>") || chunk.contains("<td>")) {
                println("Wykryto tabelę HTML — pomijam interwencję")
                result.add(chunk)
                continue
            }

            if (chunk.length <= maxChunkLen) {
                if (chunk.length < minChunkLen && result.isNotEmpty()) {
                    val lastChunk = result.last()
                    if ((lastChunk.length + chunk.length) <= maxChunkLen) {
                        result[result.size - 1] = "$lastChunk\n\n$chunk".trim()
                    } else {
                        result.add(chunk)
                    }
                } else {
                    result.add(chunk)
                }
            } else {
                val mid = chunk.length / 2

                val newlineBefore = chunk.lastIndexOf("\n", mid)
                val newlineAfter = chunk.indexOf("\n", mid)

                val splitIndex = when {
                    newlineBefore != -1 && (mid - newlineBefore) <= (newlineAfter - mid) -> newlineBefore
                    newlineAfter != -1 -> newlineAfter
                    else -> -1
                }

                if (splitIndex != -1) {
                    val firstPart = chunk.substring(0, splitIndex).trim()
                    val secondPart = chunk.substring(splitIndex + 1).trim()

                    if (firstPart.isNotBlank()) result.add(firstPart)
                    if (secondPart.isNotBlank()) result.add(secondPart)
                } else {
                    result.add(chunk)
                }
            }
        }

        val tooLongAfter = result.count { it.length > maxChunkLen }
        println("Za długie chunki po podziale (newline): $tooLongAfter")
        println("Różnica po podziale (newline): ${tooLongAfter - tooLongBefore}")
        println()



        return result
    }



    fun process() {
        val inputText = File(pureMarkdownPath).readText()
        var chunks = splitByHeader(1, listOf(inputText), minChunkLen, maxChunkLen)

        chunks = mergeTooShortChunks(chunks)

        chunks = splitByNewline(
            chunks = chunks,
            minChunkLen = minChunkLen,
            maxChunkLen = maxChunkLen
        )
        chunks = mergeTooShortChunks(chunks)

        val newPath = outputPath.replaceAfterLast("/", "removedChunks.md")
        val validatedResult = saveTooLongChunks(
            chunks = chunks,
            path = newPath,
            maxChunkLen = maxChunkLen
        )
        saveChunksToJsonFile(validatedResult)
    }
    fun mergeTooShortChunks(chunks: List<String>): List<String> {
        if (chunks.isEmpty()) return chunks

        val result = mutableListOf<String>()
        var i = 0

        while (i < chunks.size) {
            val currentChunk = chunks[i]

            if (currentChunk.length >= minChunkLen) {
                result.add(currentChunk)
                i++
                continue
            }

            if (result.isNotEmpty() && (result.last().length + currentChunk.length) <= maxChunkLen) {
                val merged = result.last() + currentChunk
                result[result.size - 1] = merged.trim()
            } else if (i + 1 < chunks.size && (currentChunk.length + chunks[i + 1].length) <= maxChunkLen) {
                val merged = currentChunk + chunks[i + 1]
                result.add(merged.trim())
                i++
            } else {
                result.add(currentChunk)
            }
            i++
        }
        return result
    }

    fun saveTooLongChunks(chunks: List<String>, path: String, maxChunkLen: Int): MutableList<String> {
        val tooLongChunks = chunks.filter { it.length > maxChunkLen }
        val validChunks = chunks.filter { it.length <= maxChunkLen }.toMutableList()


        val totalTooLong = tooLongChunks.size
        val htmlTables = tooLongChunks.count { it.contains("</tr>") || it.contains("<td>") }
        val averageLength = if (tooLongChunks.isNotEmpty()) tooLongChunks.sumOf { it.length } / tooLongChunks.size else 0

        val report = StringBuilder()
        report.appendLine("=== Analiza zbyt długich chunków ===")
        report.appendLine("Całkowita liczba: $totalTooLong")
        report.appendLine("Zawierające tabele HTML: $htmlTables")
        report.appendLine("Średnia długość: $averageLength znaków")
        report.appendLine("===================================")
        report.appendLine()

        tooLongChunks.forEachIndexed { index, chunk ->
            report.appendLine("=== Chunk $index (${chunk.length} znaków) ===")
            report.appendLine(chunk)
            report.appendLine("===================================")
            report.appendLine()
        }

        val file = File(path)
        file.parentFile.mkdirs()
        file.writeText(report.toString())

        println("Zapisano ${tooLongChunks.size} zbyt długich chunków do pliku: $path")

        return validChunks
    }



    private fun saveChunksToJsonFile(chunks: List<String>) {
        val outputFile = File(outputPath)
        outputFile.parentFile.mkdirs()
        val json = Json { prettyPrint = true }.encodeToString(ChunkJsonWrapper(chunks))
        outputFile.writeText(json)
        println("Zapisano ${chunks.size} chunków do ${outputFile.absolutePath}")
    }

    @Serializable
    data class ChunkJsonWrapper(val chunks: List<String>)
}
