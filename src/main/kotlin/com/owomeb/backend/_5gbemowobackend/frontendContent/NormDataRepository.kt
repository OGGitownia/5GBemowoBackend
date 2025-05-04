package com.owomeb.backend._5gbemowobackend.frontendContent

import jakarta.annotation.PostConstruct
import org.springframework.stereotype.Component

@Component
class NormDataRepository {

    private val releaseMap: MutableMap<String, Release> = mutableMapOf()
    private val seriesMap: MutableMap<String, List<Series>> = mutableMapOf()
    private val normsMap: MutableMap<Pair<String, String>, List<Norm>> = mutableMapOf()

    @PostConstruct
    fun initialize() {

        try {

            val releases = NormSpecificationFetcher.fetchAndParseReleases()

         releases.forEach { release ->
             releaseMap[release.releaseId] = release

                          val seriesList = NormSpecificationFetcher.getSeriesForRelease(release.releaseId) // ta linia powoduje błąd
                          seriesMap[release.releaseId] = seriesList

                          seriesList.forEach { series ->
                              val norms = NormSpecificationFetcher.getNormsForReleaseAndSeries(release.releaseId, series.seriesId)
                              normsMap[release.releaseId to series.seriesId] = norms
                          }
         }

            println("Zainicjalizowano dane norm: ${releaseMap.size} release'ów")
        } catch (e: Exception) {
            println("Błąd podczas inicjalizacji danych norm: ${e.message}")
            e.printStackTrace()
            throw e // Przerzucamy dalej, by nie maskować błędu
        }


        printDebugStructure()
    }


    fun printDebugStructure() {
        println("\n=== STRUKTURA NORM 3GPP ===")
        for ((releaseId, release) in releaseMap) {
            println("Release: $releaseId - ${release.name}")
            val seriesList = seriesMap[releaseId] ?: emptyList()
            for (series in seriesList) {
                println("        Series: ${series.seriesId} - ${series.name} (${series.description})")
                val norms = normsMap[releaseId to series.seriesId] ?: emptyList()
                for (norm in norms) {
                    println("                    Norm: ${norm.specNumber}")
                }
            }
        }
        println("=== KONIEC STRUKTURY ===\n")
    }


    fun getAllReleases(): List<Release> = releaseMap.values.toList()

    fun getSeriesForRelease(releaseId: String): List<Series> =
        seriesMap[releaseId] ?: emptyList()

    fun getNormsForReleaseAndSeries(releaseId: String, seriesId: String): List<Norm> =
        normsMap[releaseId to seriesId] ?: emptyList()
}
