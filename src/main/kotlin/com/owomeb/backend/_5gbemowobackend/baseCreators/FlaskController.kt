package com.owomeb.backend._5gbemowobackend.baseCreators

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/flask")
class FlaskController(private val flaskServerService: FlaskServerService) {

    @PostMapping("/notify")
    fun notifyServerReady(@RequestBody request: Map<String, String>): ResponseEntity<String> {
        val status = request["status"]
        if (status == "ready") {
            println("Otrzymano powiadomienie: Serwer Flask działa!")
            flaskServerService.setServerReady()
            return ResponseEntity.ok("Serwer Flask potwierdzony jako gotowy!")
        }
        return ResponseEntity.badRequest().body("Błędne powiadomienie o stanie serwera")
    }


    @PostMapping("/shutdown")
    fun shutdownFlaskServer(): ResponseEntity<String> {
        return ResponseEntity.ok(flaskServerService.shutdownServer())
    }
}
