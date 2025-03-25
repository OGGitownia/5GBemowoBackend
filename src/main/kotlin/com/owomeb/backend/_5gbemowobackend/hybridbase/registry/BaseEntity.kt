package com.owomeb.backend._5gbemowobackend.hybridbase.registry

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "bases")
data class BaseEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, unique = true)
    val sourceUrl: String = "",

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    var status: BaseStatus = BaseStatus.PENDING,

    @Column(columnDefinition = "TEXT")
    var statusMessage: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
) {
    constructor() : this(0, "", BaseStatus.PENDING, null, LocalDateTime.now())
}

enum class BaseStatus {
    PENDING,
    PROCESSING,
    READY,
    FAILED
}
