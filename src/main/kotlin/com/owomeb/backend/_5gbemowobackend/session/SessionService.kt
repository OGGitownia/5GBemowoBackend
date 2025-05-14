package com.owomeb.backend._5gbemowobackend.session

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.*

@Service
class SessionService(
    private val sessionRepository: SessionRepository
) {

    @Transactional
    fun addSession(userId: Long, isVerified: Boolean = false): String {
        val token = UUID.randomUUID().toString()
        val session = SessionEntity(token = token, userId = userId)
        sessionRepository.save(session)
        return token
    }

    @Transactional
    fun removeSession(token: String) {
        sessionRepository.deleteByToken(token)
    }

    fun validateSession(token: String): SessionEntity? {
        return sessionRepository.findByToken(token)
    }
}
