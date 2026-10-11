package com.fairShare.fairshare_backend.auth.dto;

public record LoginRequest(
        String email,
        String password
) {
}



