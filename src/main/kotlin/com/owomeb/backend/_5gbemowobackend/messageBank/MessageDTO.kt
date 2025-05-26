package com.owomeb.backend._5gbemowobackend.messageBank

data class MessageDTO(
    val id: String,
    val question: String,
    val answer: String,
    val modelName: String,
    val tuners: MutableList<String>,
    val askedAt: Long,
    val answeredAt: Long?,
    val answered: Boolean,
    val userId: Long,
    val chatId: String,
    val baseId: String
)

fun MessageEntity.toDTO(): MessageDTO = MessageDTO(
    id = id,
    question = question,
    answer = answer,
    modelName = modelName,
    tuners = tuners,
    askedAt = askedAt.toEpochMilli(),
    answeredAt = answeredAt?.toEpochMilli(),
    answered = answered,
    userId = userId,
    chatId = chatId,
    baseId = baseId
)

