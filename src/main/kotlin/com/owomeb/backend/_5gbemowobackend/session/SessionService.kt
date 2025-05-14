package com.owomeb.backend._5gbemowobackend.session

import org.springframework.stereotype.Service
import java.util.*

@Service
class SessionService(
    private val sessionRepository: SessionRepository
) {
    fun addSession(userId: Long, isVerified: Boolean = false): String {
        val token = UUID.randomUUID().toString()

        val session = SessionEntity(token = token, userId = userId)

        sessionRepository.save(session)
        return token
    }


    fun removeSession(token: String) {
        sessionRepository.deleteByToken(token)
    }

    fun validateSession(token: String): SessionEntity? {
        return sessionRepository.findByToken(token)
    }
}
