package com.owomeb.backend._5gbemowobackend.messageBank

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@RestController
@RequestMapping("/api/messages")
class MessageController(private val messageRepository: MessageRepository,
    private val messageService: MessageService
) {


    @PostMapping("/ask")
    fun handleUserQuestion(@RequestBody dto: MessageDTO): ResponseEntity<String> {
        println(dto)
        val message = MessageEntity(
            id = dto.id,
            question = dto.question,
            modelName = dto.modelName,
            tuners = dto.tuners,
            askedAt = Instant.ofEpochMilli(dto.askedAt),
            userId = dto.userId,
            chatId = dto.chatId,
            answer = dto.answer,
            answeredAt = dto.answeredAt?.let { Instant.ofEpochMilli(it) },
            answered = dto.answered,
            baseId = dto.baseId
        )
        messageRepository.save(message)
        messageService.addQuestionToQueue(message)

        return ResponseEntity.ok("Message received and processing started")
    }

    @GetMapping("/{id}")
    fun getMessage(@PathVariable id: String): ResponseEntity<MessageDTO> {
        val message = messageRepository.findById(id).orElseThrow { ResponseStatusException(HttpStatus.NOT_FOUND) }
        return ResponseEntity.ok(message.toDTO())
    }

    @GetMapping("/chat/{chatId}")
    fun getMessagesForChat(@PathVariable chatId: String): List<MessageDTO> {
        return messageRepository.findAll()
            .filter { it.chatId == chatId }
            .sortedBy { it.askedAt }
            .map { it.toDTO() }
    }
}
