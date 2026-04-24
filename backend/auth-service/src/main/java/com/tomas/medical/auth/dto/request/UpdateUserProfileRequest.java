package com.tomas.medical.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        String currentPassword,

        @Size(min = 8, max = 120, message = "newPassword must be between 8 and 120 characters")
        String newPassword
) {
}
