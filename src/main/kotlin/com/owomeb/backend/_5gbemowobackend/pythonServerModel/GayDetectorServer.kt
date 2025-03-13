package com.owomeb.backend._5gbemowobackend.pythonServerModel

// Klasa do przetwarzania pytania "Are you gay?"
class GayDetectorServer(serverName: String? = null) : PythonServerModel<String>("src/main/resources/pythonScripts/pythonServers/GayDetector.py", serverName) {
    override fun publishResult(result: String, item: String) {
        println("📌 [GayDetectorServer] Wynik: $result")
    }
}

// Klasa do sumowania dwóch liczb
data class NumberPair(val a: Int, val b: Int)

class SumCalculatorServer(serverName: String? = null) : PythonServerModel<NumberPair>("src/main/resources/pythonScripts/pythonServers/SumCalculator.py", serverName) {
    override fun publishResult(result: String, item: NumberPair) {
        println(" [SumCalculatorServer] Wynik: $result")
    }
}
