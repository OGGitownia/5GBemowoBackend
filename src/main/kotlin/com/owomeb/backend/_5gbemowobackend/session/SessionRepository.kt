package com.owomeb.backend._5gbemowobackend.session

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SessionRepository : JpaRepository<SessionEntity, String> {
    fun findByToken(token: String): SessionEntity?
    fun deleteByToken(token: String)
}
