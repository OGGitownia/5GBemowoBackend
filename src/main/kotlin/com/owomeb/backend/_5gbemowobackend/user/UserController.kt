package com.owomeb.backend._5gbemowobackend.user

import com.owomeb.backend._5gbemowobackend.token.VerificationService
import com.owomeb.backend._5gbemowobackend.user.dto.RegisterByEmailRequest
import com.owomeb.backend._5gbemowobackend.user.dto.RegisterByPhoneRequest
import com.owomeb.backend._5gbemowobackend.user.service.EmailService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val verificationService: VerificationService,
    private val emailService: EmailService
) {

    @GetMapping("/verify/email")
    fun verifyEmailToken(@RequestParam token: String): ResponseEntity<Any> {
        return try {
            verificationService.verifyEmailToken(token)
            ResponseEntity.ok("Email successfully verified")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @GetMapping("/verify/phone")
    fun verifyPhoneToken(@RequestParam token: String): ResponseEntity<Any> {
        return try {
            verificationService.verifyPhoneToken(token)
            ResponseEntity.ok("Phone number successfully verified")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }


    @PostMapping("/register/email")
    fun registerUserByEmail(@Valid @RequestBody request: RegisterByEmailRequest): ResponseEntity<Any> {
        return try {
            val newUser = userService.registerUser(
                username = request.username,
                password = request.password,
                email = request.email,
                phoneNumber = null
            )
            println("Rejestracja kogoś ")
            verificationService.initiateEmailVerification(request)
            ResponseEntity.ok("User ${newUser.username} has been successfully registered with email.")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/register/phone")
    fun registerUserByPhone(@Valid @RequestBody request: RegisterByPhoneRequest): ResponseEntity<Any> {
        return try {
            val newUser = userService.registerUser(
                username = request.username,
                password = request.password,
                email = null,
                phoneNumber = request.phoneNumber
            )
            ResponseEntity.ok("User ${newUser.username} has been successfully registered with phone number.")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }

    @PostMapping("/login/email")
    fun loginByEmail(
        @RequestParam email: String,
        @RequestParam password: String
    ): ResponseEntity<Any> {
        val user = userService.authenticateByEmail(email, password)
        return if (user.isPresent) {
            ResponseEntity.ok("Zalogowano poprawnie użytkownika: ${user.get().username}")
        } else {
            ResponseEntity.status(401).body("Niepoprawny email lub hasło")
        }
    }

    @PostMapping("/login/phone")
    fun loginByPhone(
        @RequestParam phoneNumber: String,
        @RequestParam password: String
    ): ResponseEntity<Any> {
        val user = userService.authenticateByPhoneNumber(phoneNumber, password)
        return if (user.isPresent) {
            ResponseEntity.ok("Zalogowano poprawnie użytkownika: ${user.get().username}")
        } else {
            ResponseEntity.status(401).body("Niepoprawny numer telefonu lub hasło")
        }
    }

    @DeleteMapping("/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Any> {
        return try {
            userService.deleteUser(id)
            ResponseEntity.ok("Użytkownik o ID $id został usunięty.")
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(e.message)
        }
    }
}
