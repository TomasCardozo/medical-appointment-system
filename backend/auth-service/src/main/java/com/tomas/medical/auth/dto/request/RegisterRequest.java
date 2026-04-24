package com.tomas.medical.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        @Size(max = 180, message = "email must be at most 180 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 120, message = "password must be between 8 and 120 characters")
        String password
) {
}
