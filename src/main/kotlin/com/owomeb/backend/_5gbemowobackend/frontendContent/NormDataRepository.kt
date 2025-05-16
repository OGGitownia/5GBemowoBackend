package com.owomeb.backend._5gbemowobackend.frontendContent

import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Async
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Component
import java.util.concurrent.CompletableFuture

@Component
@EnableAsync
class NormDataRepository {

    private val releaseMap: MutableMap<String, Release> = mutableMapOf()
    private val seriesMap: MutableMap<String, List<Series>> = mutableMapOf()
    private val normsMap: MutableMap<Pair<String, String>, List<Norm>> = mutableMapOf()

    @Volatile
    private var isLoaded: Boolean = false

    @PostConstruct
    fun initAsync() {
        println("Asynchronous downloading of releases, series, and norms started")
        initialize()
    }

    @Async
    fun initialize(): CompletableFuture<Void> {
        return CompletableFuture.runAsync {
            try {
                val releases = NormSpecificationFetcher.fetchAndParseReleases()

                releases.forEach { release ->
                    releaseMap[release.releaseId] = release

                    val seriesList = NormSpecificationFetcher.getSeriesForRelease(release.releaseId)
                    seriesMap[release.releaseId] = seriesList

                    seriesList.forEach { series ->
                        val norms = NormSpecificationFetcher.getNormsForReleaseAndSeries(release.releaseId, series.seriesId)
                        normsMap[release.releaseId to series.seriesId] = norms
                    }
                }

                println("Data initialization completed: ${releaseMap.size} releases successfully loaded.")
                isLoaded = true
            } catch (e: Exception) {
                println("Error during data initialization: ${e.message}")
                e.printStackTrace()
                throw e
            }
            printDebugStructure()
        }
    }

    fun printDebugStructure() {
        if (!isLoaded) {
            println("Data is still loading Please wait a moment.")
            return
        }
        println(" === 3GPP NORM STRUCTURE ===")
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
                    println("=== END OF STRUCTURE ===")
    }

    fun isDataLoaded(): Boolean = isLoaded

    fun getAllReleases(): List<Release> {
        if (!isLoaded) throw IllegalStateException("Data is still loading, please try again later.")
        return releaseMap.values.toList()
    }

    fun getSeriesForRelease(releaseId: String): List<Series> {
        if (!isLoaded) throw IllegalStateException("Data is still loading, please try again later.")
        return seriesMap[releaseId] ?: emptyList()
    }

    fun getNormsForReleaseAndSeries(releaseId: String, seriesId: String): List<Norm> {
        if (!isLoaded) throw IllegalStateException("Data is still loading, please try again later.")
        return normsMap[releaseId to seriesId] ?: emptyList()
    }
}