package com.owomeb.backend._5gbemowobackend.token

import org.springframework.data.jpa.repository.JpaRepository
import java.util.*

interface VerificationTokenRepository : JpaRepository<VerificationToken, Long> {
    fun findByTokenAndType(token: String, type: VerificationType): Optional<VerificationToken>
    fun findByUserIdAndType(userId: Long, type: VerificationType): Optional<VerificationToken>
}
