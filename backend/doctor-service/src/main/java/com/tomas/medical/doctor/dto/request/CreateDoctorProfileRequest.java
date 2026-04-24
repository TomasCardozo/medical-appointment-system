package com.tomas.medical.doctor.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateDoctorProfileRequest(
        @NotBlank(message = "fullName is required")
        @Size(max = 120, message = "fullName must be at most 120 characters")
        String fullName,

        @NotBlank(message = "specialty is required")
        @Size(max = 120, message = "specialty must be at most 120 characters")
        String specialty,

        @NotBlank(message = "licenseNumber is required")
        @Size(max = 60, message = "licenseNumber must be at most 60 characters")
        String licenseNumber,

        @Size(max = 220, message = "clinicAddress must be at most 220 characters")
        String clinicAddress,

        @Size(max = 800, message = "bio must be at most 800 characters")
        String bio
) {
}
