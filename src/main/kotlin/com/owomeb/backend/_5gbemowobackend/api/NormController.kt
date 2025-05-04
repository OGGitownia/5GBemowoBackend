package com.owomeb.backend._5gbemowobackend.api

import com.owomeb.backend._5gbemowobackend.frontendContent.*
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/norms")
class NormController(
    val  repository: NormDataRepository
) {


    @GetMapping("/releases")
    fun getAllReleases(): List<Release> {
        val result = repository.getAllReleases()
        println(result)
        return result
    }

    @GetMapping("/series/{releaseId}")
    fun getSeriesForRelease(@PathVariable releaseId: String): List<Series> {
        return repository.getSeriesForRelease(releaseId)
    }

    @GetMapping("/norms/{releaseId}/{seriesId}")
    fun getNormsForReleaseAndSeries(
        @PathVariable releaseId: String,
        @PathVariable seriesId: String
    ): List<Norm> {
        return repository.getNormsForReleaseAndSeries(releaseId, seriesId)
    }
}