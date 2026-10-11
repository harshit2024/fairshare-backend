package com.fairShare.fairshare_backend.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String name,
        String email,
        boolean emailVerified
) {}
