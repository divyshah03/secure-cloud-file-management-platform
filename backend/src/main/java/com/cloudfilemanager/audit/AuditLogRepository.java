package com.cloudfilemanager.audit;

import com.cloudfilemanager.file.File;
import com.cloudfilemanager.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLogEntry, Long> {
    Page<AuditLogEntry> findByFileOrderByCreatedAtDesc(File file, Pageable pageable);
    Page<AuditLogEntry> findByActorOrderByCreatedAtDesc(User actor, Pageable pageable);
}
