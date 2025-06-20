package com.owomeb.backend._5gbemowobackend.hybridbase.registry

data class CreateBaseRequest(
    val sourceUrl: String,
    val selectedMethod: String,
    val maxContextWindow: Int,
    val multiSearchAllowed: Boolean,
    val release: String,
    val series: String,
    val norm: String
)


