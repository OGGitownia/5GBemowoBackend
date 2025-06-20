package com.owomeb.backend._5gbemowobackend.hybridbase.registry

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "bases")
data class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val sourceUrl: String = "",

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: BaseStatus = BaseStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var statusMessage: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    val maxContextWindow: Int = 0,

    @Column(nullable = false)
    val multiSearchAllowed: Boolean = false,

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    val createdWthMethod: BaseCreatingMethods = BaseCreatingMethods.FITTED_OVERLAP,

    @Column(nullable = false)
    val release: String = "",

    @Column(nullable = false)
    val series: String = "",

    @Column(nullable = false)
    val norm: String = ""
) {
    constructor() : this(
        id = 0,
        sourceUrl = "",
        status = BaseStatus.PENDING,
        statusMessage = null,
        createdAt = LocalDateTime.now(),
        maxContextWindow = 0,
        multiSearchAllowed = false,
        createdWthMethod = BaseCreatingMethods.FITTED_OVERLAP,
        release = "",
        series = "",
        norm = ""
    )
}


enum class BaseStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
enum class BaseCreatingMethods{
    FITTED_OVERLAP,
    UNCOMPROMISINGNESS
}
