package com.cloudfilemanager.file.sharing.dto;

import java.time.LocalDateTime;

public record FilePermissionDto(
        Long id,
        Long userId,
        String userName,
        String userEmail,
        String role,
        LocalDateTime createdAt
) {
}
