package com.owomeb.backend._5gbemowobackend.session



import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "sessions")
data class SessionEntity(
    @Id
    val token: String = "",

    val userId: Long = 0,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
