package com.cloudfilemanager.file.sharing.dto;

import java.time.LocalDateTime;

public record FileShareLinkDto(
        Long id,
        String token,
        String url,
        String role,
        LocalDateTime expiresAt,
        boolean revoked,
        boolean expired,
        LocalDateTime createdAt
) {
}
