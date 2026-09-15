package com.cloudfilemanager.file.dto;

import java.time.LocalDateTime;

public record PresignedUploadResponse(
        String uploadUrl,
        String s3Key,
        LocalDateTime expiresAt
) {
}
