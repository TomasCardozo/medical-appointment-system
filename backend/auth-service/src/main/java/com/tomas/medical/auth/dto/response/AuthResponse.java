package com.tomas.medical.auth.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
