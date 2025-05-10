package com.owomeb.backend._5gbemowobackend.user

import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import java.time.LocalDateTime

@DataJpaTest
@Transactional
class UserServiceIntegrationTest @Autowired constructor(
    private val userRepository: UserRepository
) {

    private val passwordEncoder = BCryptPasswordEncoder()
    private val userService = UserService(userRepository, passwordEncoder)

    @Test
    fun `should register, authenticate, update last active and delete users correctly`() {
        val validUsers = (1..50).map {
            UserEntity(
                username = "userForIntegrationTest$it",
                password = passwordEncoder.encode("securePassword$it"),
                email = "user$it@example.com",
                phoneNumber = "123456789$it",
                createdAt = LocalDateTime.now(),
                lastActiveAt = LocalDateTime.now()
            )
        }

        val invalidUsers = (51..100).map {
            UserEntity(
                username = "user$it",
                password = "short",
                email = "not-an-email",
                phoneNumber = null,
                createdAt = LocalDateTime.now(),
                lastActiveAt = LocalDateTime.now()
            )
        }

        validUsers.forEach { userRepository.save(it) }

        assertEquals(50, userRepository.count())

        validUsers.forEach { user ->
            repeat(3) {
                val result = userService.authenticateByEmail(user.email!!, "securePassword${user.username.removePrefix("user")}")
                assertTrue(result.isPresent, "User login failed: ${user.username}")

                val updatedUser = result.get()
                assertNotNull(updatedUser.lastActiveAt)
                println("User ${updatedUser.username} has been logged successfully: ${updatedUser.lastActiveAt}")
            }
        }

        validUsers.forEach { user ->
            val result = userService.authenticateByEmail(user.email!!, "wrongPassword")
            assertFalse(result.isPresent, "Error: User logged in with an incorrect password!")
        }

        validUsers.forEach { user ->
            userRepository.delete(user)
        }

        assertEquals(0, userRepository.count(), "Users have not been deleted from the database!")

        validUsers.forEach { user ->
            val result = userService.authenticateByEmail(user.email!!, "securePassword${user.username.removePrefix("user")}")
            assertFalse(result.isPresent, "Error: Deleted user successfully logged in: ${user.username}")
        }
    }
}
