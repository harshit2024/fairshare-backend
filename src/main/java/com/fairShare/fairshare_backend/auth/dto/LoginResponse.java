package com.fairShare.fairshare_backend.auth.dto;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        long expiresIn
) {
}
