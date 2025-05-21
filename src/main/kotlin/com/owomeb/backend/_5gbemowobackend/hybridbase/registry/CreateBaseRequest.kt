package com.owomeb.backend._5gbemowobackend.hybridbase.registry

data class CreateBaseRequest(
    val sourceUrl: String,
    val selectedMethod: String,
    val userId: Long
)
