package com.owomeb.backend._5gbemowobackend.appControllers

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class AsyncBaseProcessor(
    private val baseService: BaseService
) {

    @Async
    fun processBase(baseId: Long) {
        try {
            println("Processing $baseId")
            // 1. Start procesu
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "Pobieranie dokumentu...")
            TimeUnit.SECONDS.sleep(2)

            // 2. Parsowanie dokumentu
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "Parsowanie zawartości...")
            TimeUnit.SECONDS.sleep(3)

            // 3. Generowanie embeddingów
            baseService.updateStatus(baseId, BaseStatus.PROCESSING, "Generowanie embeddingów...")
            TimeUnit.SECONDS.sleep(3)

            // 4. Zakończono
            baseService.updateStatus(baseId, BaseStatus.READY, "Baza gotowa")

        } catch (e: Exception) {
            baseService.updateStatus(baseId, BaseStatus.FAILED, "Błąd przetwarzania: ${e.message}")
            e.printStackTrace()
        }
    }
}
