package com.cloudfilemanager.file.dto;

import java.time.LocalDateTime;

public record PresignedDownloadResponse(
        String downloadUrl,
        String originalFileName,
        LocalDateTime expiresAt
) {
}
