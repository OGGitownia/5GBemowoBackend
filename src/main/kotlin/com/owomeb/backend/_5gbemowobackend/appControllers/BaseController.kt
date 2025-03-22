package com.owomeb.backend._5gbemowobackend.appControllers

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/bases")
class BaseController(
    private val baseService: BaseService,
    private val asyncBaseProcessor: AsyncBaseProcessor
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
        val norms = listOf("https://www.3gpp.org/ftp/Specs/archive/36_series/36.331/36331-e60.zip")
        return ResponseEntity.ok(norms)
    }

    @PostMapping
    fun createBase(@RequestBody request: CreateBaseRequest): ResponseEntity<BaseEntity> {

        val newBase = baseService.createBaseIfNotExists(request.sourceUrl)
        println("zlecono tworzenie1 $newBase")
        // Jeśli baza jeszcze nie była gotowa → rozpocznij przetwarzanie
        if (newBase.status == BaseStatus.PENDING) {
            println("zlecono tworzenie")
            asyncBaseProcessor.processBase(newBase.id)
        }
        return ResponseEntity.ok(newBase)
    }

    data class CreateBaseRequest(
        val sourceUrl: String
    )
}
