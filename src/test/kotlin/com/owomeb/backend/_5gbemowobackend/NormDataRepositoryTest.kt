package com.owomeb.backend._5gbemowobackend

import com.owomeb.backend._5gbemowobackend.frontendContent.NormDataRepository
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class NormDataRepositoryTest {

    @Test
    fun `initialize should fetch and store release, series, and norm data`() {
        val repo = NormDataRepository()
        repo.initialize()

        val releases = repo.getAllReleases()
        assertTrue(releases.isNotEmpty(), "Lista release'ów nie powinna być pusta")

        val sampleReleaseId = releases.first().releaseId
        val seriesList = repo.getSeriesForRelease(sampleReleaseId)
        assertTrue(seriesList.isNotEmpty(), "Lista seriesów dla release '$sampleReleaseId' nie powinna być pusta")

        val sampleSeriesId = seriesList.first().seriesId
        val norms = repo.getNormsForReleaseAndSeries(sampleReleaseId, sampleSeriesId)
        assertTrue(norms.isNotEmpty(), "Lista norm dla release '$sampleReleaseId' i series '$sampleSeriesId' nie powinna być pusta")

        println("\n▶️ DEBUG WYDRUK STRUKTURY NORM:")
        repo.printDebugStructure()
    }
}
