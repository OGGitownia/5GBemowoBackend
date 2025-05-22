package com.owomeb.backend._5gbemowobackend.messageBank


import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface MessageRepository : JpaRepository<MessageEntity, String> {
    fun findAllByChatId(chatId: String): List<MessageEntity>
    fun findAllByUserId(userId: Long): List<MessageEntity>
}
