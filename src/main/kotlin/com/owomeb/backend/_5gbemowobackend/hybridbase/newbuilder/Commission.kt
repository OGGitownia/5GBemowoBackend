package com.owomeb.backend._5gbemowobackend.hybridbase.newbuilder

import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseService
import com.owomeb.backend._5gbemowobackend.hybridbase.registry.BaseStatus

abstract class Commission(
    protected val baseId: Long
) {
    abstract fun proceed(baseService: BaseService)

    fun updateStatus(baseService: BaseService, status: BaseStatus, message: String) {
        baseService.updateStatus(baseId, status, message)
    }
}
