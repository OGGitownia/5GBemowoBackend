package com.owomeb.backend._5gbemowobackend.baseCreators;

import java.io.File

class ChunkyChunker(private val pureMarkdownPath: String, private val outputPath: String) {

    fun chunkThat() {
        val inputFile = File(pureMarkdownPath)
        if (!inputFile.exists()) {
            println("Plik źródłowy nie istnieje: $pureMarkdownPath")
            return
        }

        val allLines = inputFile.readLines()
        val linesToProcess = (allLines.size * 0.05).toInt().coerceAtLeast(1)
        val possibleChapters = mutableListOf<String>()

        for (line in allLines.take(linesToProcess)) {
            val trimmedLine = line.trim()
            val firstNumberIndex = trimmedLine.indexOfFirst { it.isDigit() }

            if (firstNumberIndex != -1) {
                val chapter = extractChapter(trimmedLine, firstNumberIndex)
                if (chapter.isNotEmpty()) {
                    possibleChapters.add(chapter)
                }
            }
        }

        println("Znalezione możliwe rozdziały:")
        possibleChapters.forEach { println(it) }

        val categorizedChapters = categorizeChaptersByFormat(possibleChapters)
        determineMostLikelyChapterSequence(categorizedChapters)
    }


    private fun extractChapter(line: String, startIndex: Int): String {
        val chapterBuilder = StringBuilder()
        var index = startIndex
        var lastCharWasSymbol = false
        var hasLetterAtEnd = false

        while (index < line.length) {
            val currentChar = line[index]

            if (currentChar.isDigit()) {
                chapterBuilder.append(currentChar)
                lastCharWasSymbol = false
                hasLetterAtEnd = false
            } else if (currentChar == '.' || currentChar == ',' || currentChar == '-') {
                if (lastCharWasSymbol) break
                chapterBuilder.append('.')
                lastCharWasSymbol = true
                hasLetterAtEnd = false
            } else if (currentChar.isLetter()) {
                if (hasLetterAtEnd) break
                hasLetterAtEnd = true
            } else if (currentChar == ' ') {
                break
            } else {
                break
            }
            index++
        }

        return chapterBuilder.toString()
    }

    fun categorizeChaptersByFormat(chapters: List<String>): List<List<String>> {
        val categorizedChapters = mutableMapOf<String, MutableList<String>>()

        for (chapter in chapters) {
            val normalizedChapter = chapter.replace(Regex("[,-]"), ".") // Zamiana separatorów na kropki


            if (normalizedChapter.startsWith("0")) continue


            val format = normalizedChapter.map {
                when {
                    it.isDigit() -> "*"
                    it == '.' -> "."
                    else -> ""
                }
            }.joinToString("").trim('.')

            categorizedChapters.computeIfAbsent(format) { mutableListOf() }.add(normalizedChapter)
        }

        val sortedCategories = categorizedChapters.values.map { chapterList ->
            chapterList.distinct()
                .filter { chapter ->
                    val parts = chapter.split(".")
                    val firstPart = parts.firstOrNull()


                    if (firstPart != null && firstPart.matches(Regex("^0\\d"))) return@filter false

                    for (part in parts) {
                        if (part.matches(Regex("^\\d{3,}$"))) return@filter false
                    }
                    true
                }
                .sortedWith(Comparator { a, b ->
                    val listA = a.split(".").mapNotNull { it.toIntOrNull() }
                    val listB = b.split(".").mapNotNull { it.toIntOrNull() }

                    val minSize = minOf(listA.size, listB.size)

                    for (i in 0 until minSize) {
                        if (listA[i] != listB[i]) {
                            return@Comparator listA[i].compareTo(listB[i])
                        }
                    }

                    return@Comparator listA.size.compareTo(listB.size)
                })
        }

        println("Rozdziały podzielone według formatów:")
        sortedCategories.forEachIndexed { index, list ->
            println("Format ${index + 1}: ${list.joinToString(", ")}")
        }

        return sortedCategories
    }

    fun chunkTextByChapter() {
        val inputFile = File(pureMarkdownPath)
        if (!inputFile.exists()) {
            println("Plik  nie istnieje: $pureMarkdownPath")
            return
        }

        val allLines = inputFile.readLines()
        val chunkedText = mutableListOf<String>()

        val chapterRegex = Regex("^[^\\d]{0,2}\\d+(\\.\\d+)+\\b") // Liczba w pierwszych trzech znakach, potem kropki i liczby
        var currentChunk = StringBuilder()

        for (line in allLines) {
            val trimmedLine = line.trim()

            if (trimmedLine.length >= 3 && chapterRegex.find(trimmedLine) != null) {
                if (currentChunk.isNotEmpty()) {
                    chunkedText.add(currentChunk.toString().trim()) // Zapisujemy poprzedni fragment
                    currentChunk = StringBuilder()
                }
            }
            currentChunk.appendLine(line)
        }

        if (currentChunk.isNotEmpty()) {
            chunkedText.add(currentChunk.toString().trim())
        }

        analyzeChunks(chunkedText)
        val preFinalChunks = splitLongChunks(chunkedText, 4048)
        analyzeChunks(preFinalChunks)
        saveChunksToFile(preFinalChunks, outputPath)

    }
    fun saveChunksToFile(chunks: List<String>, outputPath: String) {
        val outputFile = File(outputPath)

        outputFile.bufferedWriter().use { writer ->
            for (chunk in chunks) {
                val cleanedChunk = chunk
                    .replace("\n", " ")
                    .replace("\t", " ")
                    .replace(Regex("\\s+"), " ")
                    .trim()

                writer.write(cleanedChunk)
                writer.newLine()
            }
        }

        println("Plik zapisany : $outputPath")
    }




    fun analyzeChunks(chunks: List<String>) {
        val chunkLengths = chunks.map { it.length }

        val averageLength = chunkLengths.average()
        val maxLength = chunkLengths.maxOrNull() ?: 0
        val minLength = chunkLengths.minOrNull() ?: 0
        val chunksAbove4048 = chunkLengths.count { it > 4048 }
        val chunksBelow256 = chunkLengths.count { it < 256 }
        val longestChunk = chunks.maxByOrNull { it.length } ?: "Brak danych"

        println("Statystyki podziału tekstu na fragmenty:")
        println("Całkowita liczba fragmentów: ${chunks.size}")
        println("Średnia długość fragmentu: $averageLength znaków")
        println("Najdłuższy fragment: $maxLength znaków")
        println("Najkrótszy fragment: $minLength znaków")
        println("Fragmenty > 4048 znaków: $chunksAbove4048")
        println("Fragmenty < 256 znaków: $chunksBelow256")
        println("Najdłuższy fragment:\n$longestChunk")
    }

    fun splitLongChunks(chunks: List<String>, maxLength: Int): List<String> {
        val finalChunks = mutableListOf<String>()

        for (chunk in chunks) {
            if (chunk.length <= maxLength) {
                finalChunks.add(chunk)
            } else {
                finalChunks.addAll(splitRecursively(chunk, maxLength))
            }
        }

        return finalChunks
    }

    private fun splitRecursively(chunk: String, maxLength: Int): List<String> {
        if (chunk.length <= maxLength) return listOf(chunk)

        val lines = chunk.split("\n")
        val midIndex = lines.size / 2

        var splitIndex = midIndex
        while (splitIndex > 0 && lines[splitIndex].isNotBlank()) {
            splitIndex--
        }

        if (splitIndex == 0) splitIndex = midIndex

        val part1 = lines.subList(0, splitIndex).joinToString("\n").trim()
        val part2 = lines.subList(splitIndex, lines.size).joinToString("\n").trim()

        return splitRecursively(part1, maxLength) + splitRecursively(part2, maxLength)
    }








    private fun determineMostLikelyChapterSequence(categorizedChapters: List<List<String>>) {
        val mostLikelyFormat = categorizedChapters.maxByOrNull { it.size }

        if (mostLikelyFormat != null) {
            println("Najbardziej prawdopodobny format: ${mostLikelyFormat.joinToString(", ")}")

            chunkTextByChapter()
        } else {
            println("Nie znaleziono odpowiedniego formatu rozdziałów")
        }
    }

}
