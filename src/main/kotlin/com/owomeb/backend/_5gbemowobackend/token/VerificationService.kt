package com.owomeb.backend._5gbemowobackend.token

import com.owomeb.backend._5gbemowobackend.user.UserRepository
import com.owomeb.backend._5gbemowobackend.user.dto.RegisterByEmailRequest
import com.owomeb.backend._5gbemowobackend.user.dto.RegisterByPhoneRequest
import com.owomeb.backend._5gbemowobackend.user.service.EmailService
import org.springframework.stereotype.Service
import java.util.*
import jakarta.transaction.Transactional
import java.time.LocalDateTime

@Service
class VerificationService(
    private val verificationTokenRepository: VerificationTokenRepository,
    private val userRepository: UserRepository,
    private val emailService: EmailService
) {

    @Transactional
    fun initiateEmailVerification(request: RegisterByEmailRequest) {
        val user = userRepository.findByEmail(request.email)
            .orElseThrow { IllegalArgumentException("User not found with email ${request.email}") }

        val token = UUID.randomUUID().toString()
        val verificationToken = VerificationToken(
            token = token,
            type = VerificationType.EMAIL,
            user = user
        )
        verificationTokenRepository.save(verificationToken)

        emailService.sendVerificationEmail(request.email, token)
        println("Wysłano mail weryfikacyjny do ${request.email} z tokenem: $token")
    }

    @Transactional
    fun initiatePhoneVerification(request: RegisterByPhoneRequest) {
        val user = userRepository.findByPhoneNumber(request.phoneNumber)
            .orElseThrow { IllegalArgumentException("User not found with phone number ${request.phoneNumber}") }

        val token = UUID.randomUUID().toString()
        val verificationToken = VerificationToken(
            token = token,
            type = VerificationType.PHONE,
            user = user
        )
        verificationTokenRepository.save(verificationToken)

        // TODO: Wysłanie SMS-a z kodem
        println("Wysłano SMS weryfikacyjny do ${request.phoneNumber} z tokenem: $token")
    }

    @Transactional
    fun verifyEmailToken(token: String): Boolean {
        val verificationToken = verificationTokenRepository.findByTokenAndType(token, VerificationType.EMAIL)
            .orElseThrow { IllegalArgumentException("Invalid or expired email verification token") }

        if (verificationToken.expirationDate.isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(verificationToken)
            throw IllegalArgumentException("Token expired.")
        }

        val user = verificationToken.user
        user!!.emailVerified = true
        userRepository.save(user)

        verificationTokenRepository.delete(verificationToken)
        println("User ${user.username} verified his email")
        return true
    }

    @Transactional
    fun verifyPhoneToken(token: String): Boolean {
        val verificationToken = verificationTokenRepository.findByTokenAndType(token, VerificationType.PHONE)
            .orElseThrow { IllegalArgumentException("Invalid or expired phone verification token") }

        if (verificationToken.expirationDate.isBefore(LocalDateTime.now())) {
            verificationTokenRepository.delete(verificationToken)
            throw IllegalArgumentException("Token expired")
        }

        val user = verificationToken.user
        user!!.phoneNumberVerified = true
        userRepository.save(user)

        verificationTokenRepository.delete(verificationToken)
        println("User ${user.username} verified his phone number")
        return true
    }
}