package com.owomeb.backend._5gbemowobackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext

@SpringBootApplication
class Application

fun main(args: Array<String>) {
    val context: ApplicationContext = runApplication<Application>(*args)

    context.getBean(ServerStartupManager::class.java).apply {
        serverStartup()
    }
}
