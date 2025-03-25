package com.owomeb.backend._5gbemowobackend.hybridbase.builder

import com.owomeb.backend._5gbemowobackend.architectureMasterpiece.PythonServerModel
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/hybridDbCreator")
class HybridDbCreator: PythonServerModel<HybridDbCreator.QueueItem>(
    serverName =  "hybridDbCreator",
    scriptPath = "src/main/resources/pythonScripts/pythonServers/dbCreator.py",
    autoClose =  true
){

    @PostMapping("/server-ready")
    override fun markServerAsReady(@RequestBody body: Map<String, Any>): Map<String, String> {
        return super.markServerAsReady(body)
    }

    fun createDb(inputFilePath: String, outputFilePath: String, commission: CommissionForCreatingDB) {
        val queueItem = QueueItem(
            inputPath = inputFilePath,
            outputPath = outputFilePath,
            commission = commission
        )
        this.addToQueue(queueItem)
    }

    override fun publishResult(result: String, item: QueueItem) {
        item.commission.commissionStatus = CommissionStatus.HYBRID_BASED
        println("Item was: $item")
        println("Server result $serverName: $result")
    }

    data class QueueItem(val inputPath: String, val outputPath: String, val commission: CommissionForCreatingDB)
}