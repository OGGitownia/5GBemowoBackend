package com.owomeb.backend._5gbemowobackend.appControllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bases")
class BaseController(
    private val baseService: BaseService,
    private val commissionService: CommissionService
) {

    @GetMapping
    fun getAllBases(): List<BaseEntity> {
        println("getAllBases")
        return baseService.listAllBases()
    }
    @GetMapping("/id")
    fun getAllBases2(): String {
        println("getAllBases")
        return "baseService.listAllBases()"
    }

    fun deleteBaseBySourceUrl(url: String) {
        println("Próba usuwaniaDELETEBASEBYSOURCEURL")
        val base = baseService.findBySourceUrl(url)
        if (base != null) {
            baseService.delete(base)
        }
    }


    @GetMapping("/{id}")
    fun getBase(@PathVariable id: Long): ResponseEntity<BaseEntity> {
        println("getBase")
        val base = baseService.getBaseById(id)
        return if (base != null) ResponseEntity.ok(base)
        else ResponseEntity.notFound().build()
    }

    @GetMapping("/available-norms")
    fun getAvailableNorms(): ResponseEntity<List<String>> {
        println("asked")
        val norms = listOf("https://www.3gpp.org/ftp/Specs/archive/36_series/36.331/36331-e60.zip")
        return ResponseEntity.ok(norms)
    }

    @PostMapping
    fun createBase(@RequestBody request: CreateBaseRequest): ResponseEntity<BaseEntity> {
        val newBase = baseService.createBaseIfNotExists(request.sourceUrl)
        println("Zlecono tworzenie bazy: $newBase")

        if (newBase.status == BaseStatus.PENDING) {
            println("Rozpoczynam przetwarzanie normy...")
            commissionService.startCommission(newBase.id, request.sourceUrl)
        }
        return ResponseEntity.ok(newBase)
    }

    data class CreateBaseRequest(
        val sourceUrl: String
    )
}
