package com.tomas.medical.auth.dto.response;

public record InternalUserResponse(
        Long id,
        String fullName,
        String email,
        String role,
        boolean active
) {
}
