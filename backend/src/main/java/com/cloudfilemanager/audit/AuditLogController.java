package com.cloudfilemanager.audit;

import com.cloudfilemanager.audit.dto.AuditLogEntryDto;
import com.cloudfilemanager.user.User;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping("/api/v1/files/{fileId}/audit-log")
    @PreAuthorize("hasRole('USER')")
    public Page<AuditLogEntryDto> getFileAuditLog(
            @PathVariable Long fileId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal User owner) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogService.listForFile(fileId, owner, pageable);
    }

    @GetMapping("/api/v1/audit-log/me")
    @PreAuthorize("hasRole('USER')")
    public Page<AuditLogEntryDto> getMyActivity(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @AuthenticationPrincipal User user) {
        Pageable pageable = PageRequest.of(page, size);
        return auditLogService.listForActor(user, pageable);
    }
}
