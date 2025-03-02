package com.owomeb.backend._5gbemowobackend.baseCreators

import org.apache.poi.hwpf.HWPFDocument
import org.apache.poi.hwpf.usermodel.Range
import org.apache.poi.hwpf.usermodel.Table
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFTable
import org.springframework.stereotype.Component
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.system.exitProcess

@Component
class JSONManager {

    private val maxFragmentLength = 1536
    private val minFragmentLength = 256
    private val overlapPercentage = 0
    private val skipTables = true
    private val skipImages = true
    private val stats = Statistics()

    fun isJsonExists(jsonPath: String): Boolean {
        val file = File(jsonPath)
        if (!file.exists()) return false

        return try {
            val content = file.readText().trim()
            content.isNotEmpty() && content.length >= 200
        } catch (e: Exception) {
            println("Plik jest, ale jest za krótki (ten plik w formacie Json)")
            false
        }
    }

    fun createJson(docPath: String, jsonPath: String): Boolean {
        if (overlapPercentage == 100) {
            println("Takie jaja to nie z nami.")
            exitProcess(1)
        }

        return try {
            val file = File(docPath)
            if (!file.exists()) {
                println("Błąd: Plik DOC nie istnieje.")
                return false
            }

            val jsonData = when {
                docPath.endsWith(".docx") -> processDocxToJson(XWPFDocument(file.inputStream()))
                docPath.endsWith(".doc") -> processDocToJson(HWPFDocument(file.inputStream()))
                else -> {
                    println("Format zjebany: $docPath")
                    return false
                }
            }

            val jsonFile = File(jsonPath)
            jsonFile.writeText(Json.encodeToString(jsonData))

            saveStatistics()

            jsonFile.exists() && jsonFile.length() > 200
        } catch (e: Exception) {
            println("Błąd robienia JSON: ${e.message}")
            false
        }
    }

    private fun processDocxToJson(document: XWPFDocument): JsonData {
        val fragments = mutableListOf<Fragment>()
        val currentFragment = StringBuilder()
        var currentLength = 0
        val tables = document.tables // Pobranie wszystkich tabel w dokumencie

        for (paragraph in document.paragraphs) {
            var text = paragraph.text.trim()

            if (text.isEmpty()) continue

            var containsTable = false
            var containsImage = false

            // Sprawdzamy, czy po danym paragrafie jest tabela (nie w paragrafie!)
            if (!skipTables) {
                val matchingTable = tables.firstOrNull { it.text.contains(paragraph.text) }
                if (matchingTable != null) {
                    containsTable = true
                    text += "\n\n" + convertTableToMarkdown(matchingTable)
                }
            }

            // Obsługa obrazków jeśli skipImages = false
            if (!skipImages && paragraph.runs.any { it.embeddedPictures.isNotEmpty() }) {
                containsImage = true
                text += "\n\n" + handleImage()
            }

            if (currentLength + text.length > maxFragmentLength) {
                if (currentLength >= minFragmentLength) {
                    fragments.add(Fragment(currentFragment.toString()))
                    stats.totalFragments++
                    stats.totalLength += currentLength
                    if (containsTable) stats.fragmentsWithTables++
                    if (containsImage) stats.fragmentsWithImages++
                    if (currentLength > maxFragmentLength) stats.fragmentsExceededMax++
                    if (currentLength < minFragmentLength) stats.fragmentsBelowMin++

                    currentFragment.clear()
                    currentLength = 0
                }
            }

            currentFragment.append(text).append("\n\n")
            currentLength += text.length
        }

        if (currentFragment.isNotEmpty()) {
            fragments.add(Fragment(currentFragment.toString()))
            stats.totalFragments++
            stats.totalLength += currentLength
        }

        document.close()
        return JsonData(fragments)
    }

    private fun processDocToJson(document: HWPFDocument): JsonData {
        val range: Range = document.range
        val fragments = mutableListOf<Fragment>()
        val currentFragment = StringBuilder()
        var currentLength = 0

        for (i in 0 until range.numParagraphs()) {
            var text = range.getParagraph(i).text().trim()

            if (text.isEmpty()) continue

            var containsTable = false
            if (!skipTables) {
                val tableText = findTableInRange(range, i)
                if (tableText != null) {
                    containsTable = true
                    text += "\n\n" + tableText
                }
            }

            if (currentLength + text.length > maxFragmentLength) {
                if (currentLength >= minFragmentLength) {
                    fragments.add(Fragment(currentFragment.toString()))
                    stats.totalFragments++
                    stats.totalLength += currentLength
                    if (containsTable) stats.fragmentsWithTables++
                    if (currentLength > maxFragmentLength) stats.fragmentsExceededMax++
                    if (currentLength < minFragmentLength) stats.fragmentsBelowMin++

                    currentFragment.clear()
                    currentLength = 0
                }
            }

            currentFragment.append(text).append("\n\n")
            currentLength += text.length
        }

        if (currentFragment.isNotEmpty()) {
            fragments.add(Fragment(currentFragment.toString()))
            stats.totalFragments++
            stats.totalLength += currentLength
        }

        document.close()
        return JsonData(fragments)
    }



    private fun findTableInRange(range: Range, paragraphIndex: Int): String? {
        val text = StringBuilder()
        var isInsideTable = false

        for (i in paragraphIndex until range.numParagraphs()) {
            val paragraph = range.getParagraph(i)

            if (paragraph.isInTable) {
                isInsideTable = true
                text.append("| ").append(paragraph.text().trim()).append(" |\n")
            } else if (isInsideTable) {
                // Koniec tabeli, przerywamy
                break
            }
        }

        return if (text.isNotEmpty()) text.toString() else null
    }






    private fun convertTableToMarkdown(table: XWPFTable): String {
        val markdown = StringBuilder("\n")
        for (row in table.rows) {
            for (cell in row.tableCells) {
                markdown.append("| ").append(cell.text.trim()).append(" ")
            }
            markdown.append("|\n")
        }
        return markdown.toString()
    }

    private fun convertTableToMarkdown(table: Table): String {
        val markdown = StringBuilder("\n")
        for (i in 0 until table.numRows()) {
            val row = table.getRow(i)
            for (j in 0 until row.numCells()) {
                markdown.append("| ").append(row.getCell(j).text().trim()).append(" ")
            }
            markdown.append("|\n")
        }
        return markdown.toString()
    }


    private fun handleImage(): String {
        return "Uwaga obraz, może należeć do austriackiego malarza."
    }

    private fun saveStatistics() {
        val timestamp = LocalDateTime.now(ZoneId.of("Europe/Warsaw"))
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"))
        val statsFile = File("statistics_$timestamp.txt")

        val statsText = """
            --- STATYSTYKI KONWERSJI JSON ---
            Data: $timestamp
            Ilość fragmentów: ${stats.totalFragments}
            Średnia długość fragmentu: ${if (stats.totalFragments > 0) stats.totalLength / stats.totalFragments else 0}
            Ilość fragmentów z tabelami: ${stats.fragmentsWithTables}
            Ilość fragmentów ze zdjęciami: ${stats.fragmentsWithImages}
            Ilość fragmentów przekraczających maksymalną długość: ${stats.fragmentsExceededMax}
            Ilość fragmentów poniżej minimalnej długości: ${stats.fragmentsBelowMin}
        """.trimIndent()

        statsFile.writeText(statsText)
        println("📊 Statystyki zapisane do: ${statsFile.absolutePath}")
    }
}

@Serializable
data class JsonData(val fragments: MutableList<Fragment>)

@Serializable
data class Fragment(
    val content: String,
    var embeddedContent: List<Float> = emptyList() // Początkowo pusta lista
)
data class Statistics(
    var totalFragments: Int = 0,
    var totalLength: Int = 0,
    var fragmentsWithTables: Int = 0,
    var fragmentsWithImages: Int = 0,
    var fragmentsExceededMax: Int = 0,
    var fragmentsBelowMin: Int = 0
)
