package com.owomeb.backend._5gbemowobackend.pythonServerModel

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    println("\n🚀 Rozpoczynam testowanie systemu...")

    // Tworzymy instancje serwerów
    val gayDetector = GayDetectorServer("GayDetector1")
    val sumCalculator = SumCalculatorServer("SumCalculator1")

    println("\n✅ Dodajemy zapytania do GayDetectorServer...")
    gayDetector.addToQueue("Are you gay?")
    gayDetector.addToQueue("Are you a lesbian?")
    gayDetector.addToQueue("Do you support LGBT?")

    println("\n✅ Dodajemy liczby do SumCalculatorServer...")
    sumCalculator.addToQueue(NumberPair(10, 20))
    sumCalculator.addToQueue(NumberPair(5, 15))
    sumCalculator.addToQueue(NumberPair(100, 250))

    println("\n🕒 Oczekiwanie na wyniki...")
    delay(10000) // Dajemy serwerom czas na przetworzenie kolejek

    println("\n✅ Test zakończony! Sprawdź logi powyżej.")

    // Zatrzymujemy serwery po zakończeniu testu
    gayDetector.stopServer()
    sumCalculator.stopServer()

    println("\n🛑 Wszystkie serwery zostały zatrzymane.")
}
