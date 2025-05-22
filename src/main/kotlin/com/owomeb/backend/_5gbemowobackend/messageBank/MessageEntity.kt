package com.owomeb.backend._5gbemowobackend.messageBank

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDateTime

@Entity
@Table(name = "chat_messages")
data class MessageEntity(

    @Id
    @Column(name = "id", nullable = false, unique = true)
    val id: String = "",

    @Column(nullable = false, length = 10000)
    val question: String = "",

    @Column(length = 10000)
    var answer: String = "",

    @Column(nullable = false)
    val modelName: String = "",

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "chat_message_tuners", joinColumns = [JoinColumn(name = "message_id")])
    @Column(name = "tuner")
    val tuners: MutableList<String> = mutableListOf(),


    @Column(nullable = false)
    val askedAt: Instant = Instant.now(),

    @Column
    var answeredAt: Instant? = null,

    @Column(nullable = false)
    var answered: Boolean = false,

    @Column(nullable = false)
    val userId: Long = 0L,

    @Column(nullable = false)
    val chatId: String = ""
)
