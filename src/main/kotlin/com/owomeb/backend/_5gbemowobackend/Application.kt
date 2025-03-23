package com.owomeb.backend._5gbemowobackend

import kotlin.concurrent.thread
import com.owomeb.backend._5gbemowobackend.baseCreators.FlaskServerService
import com.owomeb.backend._5gbemowobackend.pythonServerModel.NewEmbeddingManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.stereotype.Component
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import java.io.File
import org.springframework.context.event.ContextRefreshedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RestController


@EnableAsync
@SpringBootApplication
class Application{

}





fun main(args: Array<String>) {
    val context: ApplicationContext = runApplication<Application>(*args)

    /*
    val filePath = """C:\gitRepositories\5GBemowo-Backend\src\main\resources\norms\36331-e60.json"""

    try {
        val jsonString = File(filePath).readText()
        val jsonObject = Json.parseToJsonElement(jsonString).jsonObject
        val problematicFragment = jsonObject["fragments"]?.jsonArray?.getOrNull(292)

        println("Problematic fragment: $problematicFragment")
    } catch (e: Exception) {
        println("Błąd podczas wczytywania JSON: ${e.message}")
    }

context.getBean(ServerStartupManager::class.java).apply {
        serverStartup()
    }
     */


}






