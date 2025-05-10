package com.owomeb.backend._5gbemowobackend.user.service

import jakarta.mail.internet.MimeMessage
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(private val mailSender: JavaMailSender) {

    fun sendVerificationEmail(to: String, token: String) {
        val message: MimeMessage = mailSender.createMimeMessage()
        val helper = MimeMessageHelper(message, true)

        helper.setTo(to)
        helper.setSubject("Email Verification - Chat 3GPP")
        helper.setText(generateEmailContent(token), true)

        mailSender.send(message)
        println("Verification email has been sent to $to")
    }

    private fun generateEmailContent(token: String): String {
        val verificationLink = "http://localhost:8080/api/users/verify/email?token=$token"
        return """
            <html>
            <body>
                <h2>Welcome to Chat 3GPP</h2>
                <p>To verify your email address, please click the link below:</p>
                <a href="$verificationLink">Verify Email</a>
                <p>This link is valid for 15 minutes.</p>
                <p>If you did not create an account, you can ignore this message</p>
            </body>
            </html>
        """.trimIndent()
    }
}
