package com.owomeb.backend._5gbemowobackend.frontendContent

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

object NormSpecificationFetcher {

    private val userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"

    fun fetchAndParseReleases(): List<Release> {
        return try {
            val document = Jsoup.connect("https://portal.3gpp.org/Releases.aspx")
                .userAgent(userAgentString)
                .timeout(10_000)
                .get()

            val html = document.html()
            val allReleases = parseReleasesFromHtml(html)

            val filtered = allReleases.filter { release ->
                val url = "https://www.3gpp.org/ftp/Specs/latest/${release.releaseId}/"
                try {
                    val response = Jsoup.connect(url)
                        .userAgent(userAgentString)
                        .timeout(5000)
                        .ignoreHttpErrors(true)
                        .execute()
                    response.statusCode() == 200
                } catch (e: Exception) {
                    false
                }
            }

            println("Znaleziono ${filtered.size} releasów z dostępem do katalogu latest/")
            filtered
        } catch (e: Exception) {
            println("Błąd podczas pobierania danych z portalu 3GPP: ${e.message}")
            emptyList()
        }
    }



    private fun parseReleasesFromHtml(html: String): List<Release> {
        val doc: Document = Jsoup.parse(html)

        val table = doc.selectFirst("table#dnn_ctr573_View_releasesTable_ctl00")
            ?: throw IllegalStateException("Nie znaleziono tabeli releases")

        val rows = table.select("tbody > tr")

        return rows.mapNotNull { row ->
            val cells = row.select("td")
            if (cells.size < 6) return@mapNotNull null

            val releaseId = cells.getOrNull(0)?.text()?.trim().ifNullOrBlank { "undefined" }

            val name = cells.getOrNull(1)
                ?.selectFirst("span")
                ?.text()
                ?.trim()
                .ifNullOrBlank { "undefined" }

            val status = cells.getOrNull(2)
                ?.text()
                ?.trim()
                .ifNullOrBlank { "undefined" }

            val startDate = cells.getOrNull(3)
                ?.text()
                ?.trim()
                .ifNullOrBlank { "undefined" }

            val endDate = cells.getOrNull(4)
                ?.text()
                ?.trim()
                .ifNullOrBlank { "undefined" }

            val closureDate = cells.getOrNull(5)
                ?.text()
                ?.trim()
                .ifNullOrBlank { "undefined" }

            Release(
                releaseId = releaseId,
                name = name,
                status = status,
                startDate = startDate,
                endDate = endDate,
                closureDate = closureDate
            )
        }
    }

    private fun String?.ifNullOrBlank(default: () -> String): String {
        return if (this.isNullOrBlank()) default() else this
    }

    fun getSeriesForRelease(releaseId: String): List<Series> {
        val url = "https://www.3gpp.org/ftp/Specs/latest/$releaseId/"
        val document = Jsoup.connect(url)
            .userAgent(userAgentString)
            .get()

        return document.select("table tbody tr")
            .mapNotNull { row ->
                try {
                    val linkElement = row.select("td:nth-child(3) a").first() ?: return@mapNotNull null
                    val name = linkElement.text()

                    // Pomijamy OpenAPI_series
                    if (name.contains("OpenAPI", ignoreCase = true)) return@mapNotNull null

                    val seriesId = name.substringBefore("_series")
                    val description = row.select("td:nth-child(4)").first()?.text()?.trim().takeIf { !it.isNullOrBlank() }

                    Series(
                        seriesId = seriesId,
                        name = name,
                        description = description ?: "undefined"
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }


    fun getNormsForReleaseAndSeries(releaseId: String, seriesId: String): List<Norm> {
        val url = "https://www.3gpp.org/ftp/Specs/latest/$releaseId/${seriesId}_series/"
        val document = Jsoup.connect(url)
            .userAgent(userAgentString)
            .get()

        return document.select("table tbody tr")
            .mapNotNull { row ->
                try {
                    val linkElement = row.selectFirst("td:nth-child(3) a") ?: return@mapNotNull null
                    val fileName = linkElement.text().trim()
                    val specNumber = fileName.removeSuffix(".zip")
                    val zipHref = linkElement.attr("href")
                    val zipUrl = if (zipHref.startsWith("http")) zipHref else url + zipHref

                    val date = row.select("td:nth-child(4)").text().trim().ifBlank { "unknown date" }
                    val size = row.select("td:nth-child(5)").text().trim().ifBlank { "unknown size" }

                    Norm(
                        specNumber = specNumber,
                        title = "undefined",
                        versions = emptyList(),
                        latestVersion = specNumber,
                        zipUrl = zipUrl,
                        date = date,
                        size = size,
                        numberOfBases = 0
                    )
                } catch (e: Exception) {
                    null
                }
            }
    }
}


data class Release(
    val releaseId: String,
    val name: String,
    val status: String,
    val startDate: String,
    val endDate: String,
    val closureDate: String
)

data class Series(
    val seriesId: String,
    val name: String,
    val description: String?
)

data class Norm(
    val specNumber: String,
    val title: String,
    val versions: List<String>,
    val latestVersion: String,
    val zipUrl: String,
    val date: String,
    val size: String,
    val numberOfBases: Int
)
