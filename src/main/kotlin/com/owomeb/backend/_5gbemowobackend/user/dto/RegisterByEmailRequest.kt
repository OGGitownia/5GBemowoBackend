package com.owomeb.backend._5gbemowobackend.user.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterByEmailRequest(
    @field:NotEmpty(message = "Username is required")
    val username: String,

    @field:NotEmpty(message = "Password is required")
    @field:Size(min = 8, message = "Password must have at least 8 characters")
    @field:Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{8,}$",
        message = "Password must contain at least one uppercase letter, one lowercase letter, one digit, and one special character"
    )
    val password: String,

    @field:Email(message = "Invalid email format")
    val email: String
)
