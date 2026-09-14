package com.cloudfilemanager.user.dto;

import com.cloudfilemanager.user.Role;

import java.time.LocalDateTime;

public record UserDTO(
        Long id,
        String name,
        String email,
        Role role,
        Boolean enabled,
        LocalDateTime emailVerifiedAt,
        LocalDateTime createdAt
) {
}
