package com.owomeb.backend._5gbemowobackend.core

import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseController
import com.owomeb.backend._5gbemowobackend.hybridbase.builder.NewMarkDowns
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.io.File
import kotlin.system.exitProcess

@Component
class ServerStartupManager(
    private val baseController: BaseController,
) {

    fun serverStartup() {
        println("Hello World!")

        //baseController.deleteBaseBySourceUrl("https://www.3gpp.org/ftp/Specs/latest/Rel-18/36_series/36331-i50.zip")
        //baseController.deleteAllBases()
    }
    @EventListener(ApplicationReadyEvent::class)
    fun onServerStart() {
        serverStartup()
    }


}
