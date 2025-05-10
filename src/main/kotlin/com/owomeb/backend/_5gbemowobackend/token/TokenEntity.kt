package com.owomeb.backend._5gbemowobackend.token

import com.owomeb.backend._5gbemowobackend.user.UserEntity
import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "verification_tokens")
data class VerificationToken(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val token: String,

    @Column(nullable = false)
    val expirationDate: LocalDateTime = LocalDateTime.now().plusMinutes(15),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: VerificationType,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    val user: UserEntity
)

enum class VerificationType {
    EMAIL, PHONE
}