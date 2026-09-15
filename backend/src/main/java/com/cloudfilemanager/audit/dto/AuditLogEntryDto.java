package com.cloudfilemanager.audit.dto;

import java.time.LocalDateTime;

public record AuditLogEntryDto(
        Long id,
        Long actorUserId,
        String actorEmail,
        String action,
        Long fileId,
        String fileName,
        String metadata,
        LocalDateTime createdAt
) {
}
