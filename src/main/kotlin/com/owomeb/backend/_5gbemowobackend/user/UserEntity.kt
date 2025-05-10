package com.owomeb.backend._5gbemowobackend.user


import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "users")
data class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = true, unique = true)
    val email: String? = null,

    @Column(nullable = true, unique = true)
    val phoneNumber: String? = null,

    @Column(nullable = false, unique = true)
    val username: String = "",

    @Column(nullable = false)
    val password: String = "",

    @Column
    val avatarPath: String? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(nullable = false)
    var lastActiveAt: LocalDateTime = LocalDateTime.now(),

    @ElementCollection(fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_roles", joinColumns = [JoinColumn(name = "user_id")])
    val roles: Set<UserRole> = setOf(UserRole.USER),

    @Column(nullable = false)
    val isActive: Boolean = true,

    @Column(nullable = false)
    var emailVerified: Boolean = false,

    @Column(nullable = false)
    var phoneNumberVerified: Boolean = false
)
