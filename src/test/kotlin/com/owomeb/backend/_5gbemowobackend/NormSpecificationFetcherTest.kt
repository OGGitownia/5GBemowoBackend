package com.owomeb.backend._5gbemowobackend

import com.owomeb.backend._5gbemowobackend.frontendContent.NormSpecificationFetcher
import org.junit.jupiter.api.Test
import kotlin.test.*

class NormSpecificationFetcherTest {

    @Test
    fun `fetchAndParseReleases should return non-empty list with valid data`() {
        val releases = NormSpecificationFetcher.fetchAndParseReleases()
        println("Znaleziono ${releases.size} release'ów")

        assertTrue(releases.size > 26, "Powinno być więcej niż 26 release'ów")

        releases.forEach { release ->
            assertNotEquals("undefined", release.releaseId, "releaseId shouldn't be 'undefined'")
            assertNotEquals("undefined", release.name, "release name shouldn't be 'undefined'")
        }
    }

    @Test
    fun `getSeriesForRelease should return valid series list for known releases`() {
        val testReleaseIds = listOf("Rel-17", "Rel-18", "Rel-19")

        for (releaseId in testReleaseIds) {
            val series = NormSpecificationFetcher.getSeriesForRelease(releaseId)
            println("Release: $releaseId -> Found ${series.size} series")

            assertTrue(series.isNotEmpty(), "Series list for $releaseId should not be empty")

            series.forEach {
                assertNotEquals("", it.seriesId, "seriesId should not be empty")
                assertNotEquals("", it.name, "series name should not be empty")
                assertNotNull(it.description, "series description should not be null")
            }
        }
    }

    @Test
    fun `getNormsForReleaseAndSeries should return valid norms for known release and series`() {
        val releaseId = "Rel-18"
        val seriesId = "36"

        val norms = NormSpecificationFetcher.getNormsForReleaseAndSeries(releaseId, seriesId)
        println("Release: $releaseId / Series: $seriesId -> Found ${norms.size} norms")

        assertTrue(norms.isNotEmpty(), "Norm list for $releaseId/$seriesId should not be empty")


        norms.forEach { norm ->
            assertTrue(norm.zipUrl.endsWith(".zip"), "Invalid zipUrl: ${norm.zipUrl}")
            assertTrue(norm.date.isNotBlank(), "Date should not be blank")
            assertTrue(norm.size.isNotBlank(), "Size should not be blank")
            assertNotNull(norm.title, "Title should not be null") // nie wymuszamy 'undefined'
        }
    }
}
