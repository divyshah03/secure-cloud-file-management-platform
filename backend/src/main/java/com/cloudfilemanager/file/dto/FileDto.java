package com.cloudfilemanager.file.dto;

import java.time.LocalDateTime;

public record FileDto(
        Long id,
        String fileName,
        String originalFileName,
        Long fileSize,
        String contentType,
        String downloadUrl,
        LocalDateTime createdAt,
        Long ownerId,
        String role
) {
}
