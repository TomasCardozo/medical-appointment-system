package com.tomas.medical.auth.dto.response;

public record UserMeResponse(
        Long id,
        String fullName,
        String email,
        String role
) {
}
