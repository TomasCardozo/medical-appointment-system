package com.tomas.medical.appointment.client.dto;

public record InternalUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        boolean active
) {
}
