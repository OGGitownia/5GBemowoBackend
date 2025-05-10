package com.owomeb.backend._5gbemowobackend.user


import jakarta.transaction.Transactional
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.Optional

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder
) {

    @Transactional
    fun registerUser(username: String, password: String, email: String?, phoneNumber: String?): UserEntity {
        if (email != null && userRepository.existsByEmail(email)) {
            throw IllegalArgumentException("User with email: $email already exists")
        }

        if (phoneNumber != null && userRepository.existsByPhoneNumber(phoneNumber)) {
            throw IllegalArgumentException("User with phone number: $phoneNumber already exists")
        }

        if (userRepository.existsByUsername(username)) {
            throw IllegalArgumentException("User with username $username already exists")
        }

        val hashedPassword = passwordEncoder.encode(password)

        val newUser = UserEntity(
            email = email,
            phoneNumber = phoneNumber,
            username = username,
            password = hashedPassword,
            createdAt = LocalDateTime.now(),
            lastActiveAt = LocalDateTime.now()
        )
        return userRepository.save(newUser)
    }

    fun findByEmail(email: String): Optional<UserEntity> {
        return userRepository.findByEmail(email)
    }

    fun findByPhoneNumber(phoneNumber: String): Optional<UserEntity> {
        return userRepository.findByPhoneNumber(phoneNumber)
    }

    @Transactional
    fun updateLastActive(user: UserEntity) {
        user.lastActiveAt = LocalDateTime.now()
        userRepository.save(user)
    }

    @Transactional
    fun deleteUser(userId: Long) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId)
        } else {
            throw IllegalArgumentException("User with id $userId does not exist.")
        }
    }
    @Transactional
    fun authenticateByEmail(email: String, password: String): Optional<UserEntity> {
        val userOpt = userRepository.findByEmail(email)

        if (userOpt.isPresent) {
            val user = userOpt.get()
            if (passwordEncoder.matches(password, user.password)) {
                user.lastActiveAt = LocalDateTime.now()
                userRepository.save(user)
                return Optional.of(user)
            }
        }

        return Optional.empty()
    }

    @Transactional
    fun authenticateByPhoneNumber(phoneNumber: String, password: String): Optional<UserEntity> {
        val userOpt = userRepository.findByPhoneNumber(phoneNumber)

        if (userOpt.isPresent) {
            val user = userOpt.get()
            if (passwordEncoder.matches(password, user.password)) {
                user.lastActiveAt = LocalDateTime.now()
                userRepository.save(user)
                return Optional.of(user)
            }
        }

        return Optional.empty()
    }
}
