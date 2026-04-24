package com.tomas.medical.doctor.dto.response;

public record InternalDoctorOwnerResponse(
        Long id,
        String ownerEmail,
        boolean active
) {
}
