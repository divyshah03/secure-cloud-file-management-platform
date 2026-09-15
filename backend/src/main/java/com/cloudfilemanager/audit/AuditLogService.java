package com.cloudfilemanager.audit;

import com.cloudfilemanager.audit.dto.AuditLogEntryDto;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import com.cloudfilemanager.file.File;
import com.cloudfilemanager.file.FileRepository;
import com.cloudfilemanager.user.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogService.class);

    private final AuditLogRepository auditLogRepository;
    private final FileRepository fileRepository;

    public AuditLogService(AuditLogRepository auditLogRepository, FileRepository fileRepository) {
        this.auditLogRepository = auditLogRepository;
        this.fileRepository = fileRepository;
    }

    @Transactional
    public void record(User actor, AuditAction action, File file, String metadata) {
        try {
            AuditLogEntry entry = new AuditLogEntry(
                    actor,
                    actor != null ? actor.getEmail() : null,
                    action,
                    file,
                    file != null ? file.getOriginalFileName() : null,
                    metadata
            );
            auditLogRepository.save(entry);
        } catch (Exception e) {
            // Audit logging must never break the underlying operation it's observing.
            logger.error("Failed to write audit log entry for action {}", action, e);
        }
    }

    public Page<AuditLogEntryDto> listForFile(Long fileId, User owner, Pageable pageable) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("File not found with id: " + fileId);
        }
        return auditLogRepository.findByFileOrderByCreatedAtDesc(file, pageable).map(this::toDto);
    }

    public Page<AuditLogEntryDto> listForActor(User actor, Pageable pageable) {
        return auditLogRepository.findByActorOrderByCreatedAtDesc(actor, pageable).map(this::toDto);
    }

    private AuditLogEntryDto toDto(AuditLogEntry entry) {
        return new AuditLogEntryDto(
                entry.getId(),
                entry.getActor() != null ? entry.getActor().getId() : null,
                entry.getActorEmail(),
                entry.getAction().name(),
                entry.getFile() != null ? entry.getFile().getId() : null,
                entry.getFileNameSnapshot(),
                entry.getMetadata(),
                entry.getCreatedAt()
        );
    }
}
