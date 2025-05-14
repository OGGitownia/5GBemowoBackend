package com.owomeb.backend._5gbemowobackend.user

import com.owomeb.backend._5gbemowobackend.token.VerificationService
import com.owomeb.backend._5gbemowobackend.user.dto.RegisterByEmailRequest
import com.owomeb.backend._5gbemowobackend.user.dto.RegisterByPhoneRequest
import com.owomeb.backend._5gbemowobackend.user.service.EmailService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.owomeb.backend._5gbemowobackend.session.SessionService


@RestController
@RequestMapping("/api/users")
class UserController(
    private val userService: UserService,
    private val verificationService: VerificationService,
    private val sessionService: SessionService
) {

    @GetMapping("/session/{token}")
    fun getUserSession(@PathVariable token: String): ResponseEntity<UserEntity> {
        val session = sessionService.validateSession(token)
        if (session == null) {
            return ResponseEntity.status(401).build()
        }

        val userOptional = userService.findById(session.userId)

        return if (userOptional.isPresent) {
            val user = userOptional.get()
            user.password = "********"
            ResponseEntity.ok(user)
        } else {
            ResponseEntity.status(404).build()
        }
    }

    @PostMapping("/session/new/{userId}")
    fun createNewSession(@PathVariable userId: Long): ResponseEntity<Map<String, String>> {
        val userOptional = userService.findById(userId)

        if (userOptional.isEmpty) {
            return ResponseEntity.status(401).body(mapOf("error" to "Unauthorized - User not found"))
        }

        val user = userOptional.get()

        val newToken = sessionService.addSession(user.id)

        if (newToken == null) {
            return ResponseEntity.status(500).body(mapOf("error" to "Failed to generate session token"))
        }

        return ResponseEntity.ok(mapOf("token" to newToken))
    }

    @GetMapping("/verify/email")
    fun verifyEmailToken(@RequestParam token: String): ResponseEntity<Map<String, String>> {
        return try {
            println("token: $token")
            verificationService.verifyEmailToken(token)
            ResponseEntity.ok(mapOf("message" to "Email successfully verified"))
        } catch (e: IllegalArgumentException) {
            println("Bad request")
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Verification failed")))
        }
    }



    @GetMapping("/verify/phone")
    fun verifyPhoneToken(@RequestParam token: String): ResponseEntity<Map<String, String>> {
        return try {
            verificationService.verifyPhoneToken(token)
            ResponseEntity.ok(mapOf("message" to "Phone number successfully verified"))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Verification failed")))
        }
    }



    @PostMapping("/register/email")
    fun registerUserByEmail(@Valid @RequestBody request: RegisterByEmailRequest): ResponseEntity<Map<String, String>> {
        return try {
            val newUser = userService.registerUser(
                username = request.username,
                password = request.password,
                email = request.email,
                phoneNumber = null
            )
            println("Rejestracja kogoś ")
            verificationService.initiateEmailVerification(request)


            ResponseEntity.ok(mapOf(
                "message" to "User ${newUser.username} has been successfully registered with email.",
                "id" to newUser.id.toString(),
                "email" to (newUser.email ?: ""),
                "username" to (newUser.username)
            ))

        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf("error" to (e.message ?: "Unknown error")))
        }
    }


    @PostMapping("/register/phone")
    fun registerUserByPhone(@Valid @RequestBody request: RegisterByPhoneRequest): ResponseEntity<Map<String, String>> {
        return try {
            val newUser = userService.registerUser(
                username = request.username,
                password = request.password,
                email = null,
                phoneNumber = request.phoneNumber
            )
            ResponseEntity.ok(mapOf(
                "message" to "User ${newUser.username} has been successfully registered with phone number.",
                "id" to newUser.id.toString(),
                "phoneNumber" to (newUser.phoneNumber ?: ""),
                "username" to newUser.username
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "error" to (e.message ?: "Unknown error during registration")
            ))
        }
    }


    @PostMapping("/login/email")
    fun loginByEmail(
        @RequestParam email: String,
        @RequestParam password: String
    ): ResponseEntity<Map<String, String>> {
        val user = userService.authenticateByEmail(email, password)

        return if (user.isPresent) {
            ResponseEntity.ok(
                mapOf(
                    "message" to "Zalogowano poprawnie użytkownika",
                    "username" to user.get().username
                )
            )
        } else {
            ResponseEntity.status(401).body(
                mapOf(
                    "error" to "Niepoprawny email lub hasło"
                )
            )
        }
    }


    @PostMapping("/login/phone")
    fun loginByPhone(
        @RequestParam phoneNumber: String,
        @RequestParam password: String
    ): ResponseEntity<Map<String, String>> {
        val user = userService.authenticateByPhoneNumber(phoneNumber, password)

        return if (user.isPresent) {
            ResponseEntity.ok(mapOf(
                "message" to "Zalogowano poprawnie użytkownika",
                "username" to user.get().username
            ))
        } else {
            ResponseEntity.status(401).body(mapOf(
                "error" to "Niepoprawny numer telefonu lub hasło"
            ))
        }
    }

    @DeleteMapping("delete/{id}")
    fun deleteUser(@PathVariable id: Long): ResponseEntity<Map<String, String>> {
        return try {
            userService.deleteUser(id)
            ResponseEntity.ok(mapOf(
                "message" to "Użytkownik o ID $id został usunięty."
            ))
        } catch (e: IllegalArgumentException) {
            ResponseEntity.badRequest().body(mapOf(
                "error" to (e.message ?: "Nieznany błąd podczas usuwania użytkownika.")
            ))
        }
    }

}
