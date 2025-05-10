package com.owomeb.backend._5gbemowobackend.hybridbase.builder


import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File


class FinalChunker(
    private val pureMarkdownPath: String,
    private val outputPath: String,
    private val minChunkLen: Int = 200,
    private val maxChunkLen: Int = 400
) {

    fun splitByHeader(level: Int, chunks: List<String>, minWords: Int, maxWords: Int): List<String> {
        println()
        val pattern = "^${"#".repeat(level)} ".toRegex(RegexOption.MULTILINE)

        val tooLongBefore = chunks.count { it.split("\\s+".toRegex()).size > maxWords }
        println("Za długie chunki przed podziałem $pattern: $tooLongBefore")

        val result = mutableListOf<String>()

        for (chunk in chunks) {
            val wordCount = chunk.split("\\s+".toRegex()).size

            if (wordCount < minWords && result.isNotEmpty()) {
                // 🔗 Łączymy za krótkie, jeśli to możliwe
                val lastChunk = result.last()
                val lastWordCount = lastChunk.split("\\s+".toRegex()).size

                if ((lastWordCount + wordCount) <= maxWords) {
                    result[result.size - 1] = "$lastChunk\n\n$chunk".trim()
                } else {
                    result.add(chunk)
                }
            } else if (wordCount > maxWords) {
                // 🔍 Podział dużego chunka po nagłówkach
                val splitChunks = chunk.split(pattern)
                    .map { it.trim() }
                    .filter { it.isNotBlank() }

                val cleanedChunks = splitChunks.mapIndexed { index, part ->
                    if (index == 0) part
                    else part.substringAfter("\n").trim()
                }

                println("Dzielimy chunk na ${cleanedChunks.size} części")
                result.addAll(cleanedChunks)
            } else {
                result.add(chunk)
            }
        }

        val tooLongAfter = result.count { it.split("\\s+".toRegex()).size > maxWords }
        println("Za długie chunki po podziale: $tooLongAfter")
        println("Różnica po podziale $pattern: ${tooLongAfter - tooLongBefore}")
        println()

        return if (tooLongAfter > 0 && tooLongAfter != tooLongBefore) {
            splitByHeader(level + 1, result, minWords, maxWords)
        } else {
            result
        }
    }

    fun splitTextByAllHeaders(input: Pair<String, Int>): List<Pair<String, Int>> {
        val (text, level) = input
        val nextLevel = level + 1
        val pattern = "^${"#".repeat(nextLevel)}\\s+".toRegex(RegexOption.MULTILINE)

        val splitChunks = text.split(pattern)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (splitChunks.size == 1) {
            return listOf(Pair(text, level))
        }

        val result = mutableListOf<Pair<String, Int>>()
        for (chunk in splitChunks) {
            result.addAll(splitTextByAllHeaders(Pair(chunk, nextLevel)))
        }
        return result
    }
    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)


    fun processChunks(chunks: List<Pair<String, Int>>): List<Quadruple<String, String, Int, String>> {

        val result = mutableListOf<Quadruple<String, String, Int, String>>()
        val hierarchy = mutableMapOf<Int, String>()

        chunks.forEach { (chunk, level) ->
            val lines = chunk.lines()
            val title = lines.firstOrNull()?.take(100)?.trim() ?: "Untitled"
            val content = lines.drop(1).joinToString("\n").trim()

            hierarchy[level] = title

            val mergedTitle = (0..level).mapNotNull { hierarchy[it] }.joinToString(", ")

            if (content.isNotBlank()) {
                result.add(Quadruple(title, content, level, mergedTitle))
            } else {
                println("Usuwam pusty chunk: $title")
            }
        }
        return result
    }





    private fun printChunkInfo(chunks: List<Pair<String, Int>>) {
        println("\n**Analiza chunków:**")

        // Obliczamy liczbę słów w każdym chunku
        val wordCounts = chunks.map { (chunk, _) ->
            chunk.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        }

        // Obliczenia statystyczne
        val averageLength = wordCounts.average()
        val aboveMaxLen = wordCounts.count { it > maxChunkLen }
        val belowMinLen = wordCounts.count { it < minChunkLen }

        // Znajdowanie najdłuższych i najkrótszych chunków
        val longestChunks = wordCounts.sortedDescending().take(10)
        val shortestChunks = wordCounts.sorted().take(10)

        println("🔹 Średnia długość chunków (słowami): %.2f".format(averageLength))
        println("🔹 Liczba chunków powyżej $maxChunkLen słów: $aboveMaxLen")
        println("🔹 Liczba chunków poniżej $minChunkLen słów: $belowMinLen")

        println("\n**10 Najdłuższych chunków (ilość słów):**")
        longestChunks.forEachIndexed { index, len ->
            println("#${index + 1}: $len słów")
        }

        println("\n**10 Najkrótszych chunków (ilość słów):**")
        shortestChunks.forEachIndexed { index, len ->
            println("#${index + 1}: $len słów")
        }

        println("\n**Nagłówki chunków (pierwsze linie tekstu):**")
        chunks.forEachIndexed { index, (chunk, level) ->
            val firstLine = chunk.lines().firstOrNull()?.take(100) ?: "Brak danych"
            //println("#$index [Poziom $level]: $firstLine")
        }
        val oneLiners = chunks.count { (chunk, lvl) ->
            chunk.lines().size == 1
        }
        println("One liners $oneLiners")

    }

    private fun printQuadrupleInfo(chunks: List<Quadruple<String, String, Int, String>>) {
        println("\n**Analiza chunków:**")

        println("Ilość chunków ${chunks.size}")
        // Obliczamy liczbę słów w każdym fragmencie
        val wordCounts = chunks.map { (title, content, _, _) ->
            content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
        }

        // Obliczenia statystyczne
        val averageLength = wordCounts.average()
        val aboveMaxLen = wordCounts.count { it > maxChunkLen }
        val belowMinLen = wordCounts.count { it < minChunkLen }

        // Znajdowanie najdłuższych i najkrótszych chunków
        val longestChunks = chunks.zip(wordCounts)
            .sortedByDescending { it.second }
            .take(10)

        val shortestChunks = chunks.zip(wordCounts)
            .sortedBy { it.second }
            .take(10)

        println("🔹 Średnia długość chunków (słowami): %.2f".format(averageLength))
        println("🔹 Liczba chunków powyżej $maxChunkLen słów: $aboveMaxLen")
        println("🔹 Liczba chunków poniżej $minChunkLen słów: $belowMinLen")

        println("\n📌 **10 Najdłuższych chunków (ilość słów):**")
        longestChunks.forEachIndexed { index, (chunk, len) ->
            println("#${index + 1}: $len słów - ${chunk.first}")
        }

        println("\n📌 **Wszystkie chunki poniżej $minChunkLen słów:**")

        // Filtrujemy tylko te chunki, które mają mniej niż `minChunkLen` słów
        val shortChunks = chunks.filter { (_, content, _, _) ->
            content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size < 20
        }

        // Wyświetlamy pełne informacje
        shortChunks.forEachIndexed { index, (title, content, level, mergedTitle) ->
            val wordCount = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
            println("\n🔹 Chunk #$index [Level $level]")
            println("Title: $title")
            println("Merged Title: $mergedTitle")
            println("Liczba słów: $wordCount")
            println("Treść:\n$content")
            println("----")
        }

        println("\n📌 **Nagłówki chunków z poziomami:**")
        chunks.forEachIndexed { index, (title, _, level, mergedTitle) ->
           // println("#$index [Poziom $level] |||  Merged Title: $mergedTitle")
        }

        val oneLiners = chunks.count { (title, content, _, _) ->
            content.lines().size == 1
        }
        println("\n🔹 One-liners (jedna linia w treści): $oneLiners")
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


    private fun mergeShortChunks(chunks: List<Quadruple<String, String, Int, String>>): List<Quadruple<String, String, Int, String>> {
        val result = mutableListOf<Quadruple<String, String, Int, String>>()
        var tempContent = ""
        var tempMergedTitle = ""
        var tempLevel: Int? = null

        for ((title, content, level, mergedTitle) in chunks) {
            val wordCount = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }.size

            if (wordCount < minChunkLen) {
                // 🔗 Jeśli za krótki, dodajemy jego tytuł i zawartość do tymczasowych danych
                tempContent += if (tempContent.isEmpty()) "$title\n$content" else "\n\n$title\n$content"
                tempMergedTitle = mergedTitle
                if (tempLevel == null) tempLevel = level
            } else {
                // 📝 Jeżeli mamy coś w cache, dołączamy to do bieżącego chunka
                if (tempContent.isNotEmpty()) {
                    val newContent = tempContent + "\n\n" + content
                    result.add(Quadruple(title, newContent.trim(), level, tempMergedTitle))

                    // 🔄 Czyścimy cache
                    tempContent = ""
                    tempMergedTitle = ""
                    tempLevel = null
                } else {
                    // 🔹 Jeśli jest wystarczająco długi, dodajemy normalnie
                    result.add(Quadruple(title, content, level, mergedTitle))
                }
            }
        }

        // 🗑️ Jeśli zostało coś w cache, dopisujemy na końcu
        if (tempContent.isNotEmpty() && tempLevel != null) {
            result.add(Quadruple("Merged Chunk", tempContent.trim(), tempLevel, tempMergedTitle))
        }

        return result
    }

    fun splitLongChunk(
        chunks: List<Quadruple<String, String, Int, String>>,
        desirableLen: Int
    ): List<Quadruple<String, String, Int, String>> {

        val minOverlap = desirableLen / 10
        val result = mutableListOf<Quadruple<String, String, Int, String>>()

        for ((title, content, level, mergedTitle) in chunks) {
            val words = content.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            // ✂️ Warunek podziału - tylko jeśli długość przekracza 135% desirableLen
            if (words.size <= (desirableLen * 1.35).toInt()) {
                result.add(Quadruple(title, content, level, mergedTitle))
                continue
            }

            // ✅ Obliczanie ilości słów, które dochodzą do każdego fragmentu
            val additionalWords = mergedTitle.split("\\s+".toRegex()).size + 10 + 7
            val availableWords = desirableLen - additionalWords

            if (availableWords <= 0) {
                println("⚠️ ERROR: mergedTitle + additional info exceeds desirable length. Skipping chunk: $title")
                continue
            }

            // ✅ Obliczanie ilości fragmentów
            val fragmentCount = Math.ceil(words.size / availableWords.toDouble()).toInt()

            // ✅ Obliczenie optymalnego overlapu
            val overlap = Math.max(minOverlap, (fragmentCount * availableWords - words.size) / (fragmentCount - 1))

            if (overlap < minOverlap) {
                println("⚠️ ERROR: Calculated overlap is smaller than minimum allowed. Skipping chunk: $title")
                continue
            }

            println("✅ Splitting chunk: $title into $fragmentCount parts, overlap: $overlap")

            // Generowanie nowych fragmentów
            for (i in 0 until fragmentCount) {
                val start = i * (availableWords - overlap)
                val end = minOf(start + availableWords, words.size)

                if (start >= end) {
                    println("⚠️ Invalid range detected. start: $start, end: $end, chunk skipped.")
                    break
                }

                val fragmentWords = words.subList(start, end)
                val fragmentText = fragmentWords.joinToString(" ")

                // Formatowanie nowego tekstu
                val formattedText = """
            This part of the chunk exists mainly for context: $mergedTitle
            (This fragment is part of the section, it is fragment ${i + 1}/$fragmentCount)
            
            $fragmentText
            
            ${if (i < fragmentCount - 1) "(Below is part of fragment ${i + 2}/$fragmentCount)" else ""}
            """.trimIndent()

                val newTitle = "$title [Fragment ${i + 1}/$fragmentCount]"
                result.add(Quadruple(newTitle, formattedText, level, mergedTitle))
            }
        }

        return result
    }

    fun process() {
        val inputText = File(pureMarkdownPath).readText()
        //var chunks = splitByHeader(1, listOf(inputText), minChunkLen, maxChunkLen)
        val chunks2 = splitTextByAllHeaders(Pair(inputText, 0))

        /*
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

         */
        printChunkInfo(chunks2)
        val processedChunks = processChunks(chunks2)
        printQuadrupleInfo(processedChunks)
        val merged = mergeShortChunks(processedChunks)
        printQuadrupleInfo(merged)
        val trimmedTittles = trimLongMergedTitles(
            merged,
            maxWords = (maxChunkLen + minChunkLen)/4
        )

        val split = splitLongChunk(trimmedTittles, (maxChunkLen + minChunkLen)/2)

        printQuadrupleInfo(split)
        //analyzeStrings(chunks, maxChunkLen, minChunkLen)
        saveChunksToJsonFile(
            split,
            outputPath = outputPath
        )
    }

    fun trimLongMergedTitles(
        chunks: List<Quadruple<String, String, Int, String>>,
        maxWords: Int
    ): List<Quadruple<String, String, Int, String>> {

        println("Trimmng titles")
        val result = mutableListOf<Quadruple<String, String, Int, String>>()

        for ((title, content, level, mergedTitle) in chunks) {
            val mergedTitleWords = mergedTitle.split("\\s+".toRegex()).filter { it.isNotEmpty() }

            var newMergedTitle = mergedTitle

            // Sprawdzanie ilości słów w mergedTitle
            if (mergedTitleWords.size > maxWords) {
                val cutLength = mergedTitleWords.size - maxWords
                newMergedTitle = mergedTitleWords.takeLast(maxWords).joinToString(" ")
                println("✂️ MergedTitle trimmed by $cutLength words: [$mergedTitle] → [$newMergedTitle]")
            }

            // Dodanie nowej czwórki do listy wynikowej
            result.add(Quadruple(title, content, level, newMergedTitle))
        }

        return result
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


    fun analyzeStrings(
        strings: List<String>,
        upperWordThreshold: Int,
        lowerWordThreshold: Int
    ) {
        val totalStrings = strings.size

        // Obliczamy liczbę słów dla każdego stringa tylko raz
        val wordCounts = strings.map { str ->
            str.split("\\s+".toRegex())
                .filter { word -> word.isNotEmpty() }
                .size
        }

        // Średnia liczba słów
        val averageWords = if (wordCounts.isNotEmpty()) wordCounts.average() else 0.0

        // Liczymy stringi powyżej górnego progu
        val exceedingThreshold = wordCounts.count { it > upperWordThreshold }

        // Liczymy stringi poniżej dolnego progu
        val belowThreshold = wordCounts.count { it < lowerWordThreshold }

        println("🔹 Całkowita liczba stringów: $totalStrings")
        println("🔹 Średnia liczba słów w stringach: %.2f".format(averageWords))
        println("🔹 Liczba stringów z > $upperWordThreshold słów: $exceedingThreshold")
        println("🔹 Liczba stringów z < $lowerWordThreshold słów: $belowThreshold")
    }





    private fun saveChunksToJsonFile(chunks: List<Quadruple<String, String, Int, String>>, outputPath: String) {
        val outputFile = File(outputPath)
        outputFile.parentFile.mkdirs()

        // Konwertujemy Quadruple na ChunkJson
        val jsonChunks = chunks.map { (title, content, level, mergedTitle) ->
            ChunkJson(title, content, level, mergedTitle)
        }

        // Tworzymy wrapper JSON
        val json = Json { prettyPrint = true }.encodeToString(ChunkJsonWrapper(jsonChunks))

        // Zapis do pliku
        outputFile.writeText(json)
        println("Zapisano ${chunks.size} chunków do ${outputFile.absolutePath}")
    }

    @Serializable
    data class ChunkJsonWrapper(val chunks: List<ChunkJson>)

    @Serializable
    data class ChunkJson(
        val title: String,
        val content: String,
        val level: Int,
        val mergedTitle: String
    )
}
