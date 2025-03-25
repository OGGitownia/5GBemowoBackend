package com.owomeb.backend._5gbemowobackend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.annotation.EnableAsync


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






