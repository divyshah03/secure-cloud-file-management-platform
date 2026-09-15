package com.cloudfilemanager.audit;

import com.cloudfilemanager.file.File;
import com.cloudfilemanager.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_log")
public class AuditLogEntry {

    @Id
    @SequenceGenerator(name = "audit_log_id_seq", sequenceName = "audit_log_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "audit_log_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private User actor;

    // Denormalized so the trail stays meaningful even after the actor's account is gone.
    @Column(name = "actor_email")
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id")
    private File file;

    // Denormalized so the trail stays meaningful even after the file itself is deleted.
    @Column(name = "file_name_snapshot")
    private String fileNameSnapshot;

    @Column(length = 1000)
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public AuditLogEntry() {
    }

    public AuditLogEntry(User actor, String actorEmail, AuditAction action, File file,
                          String fileNameSnapshot, String metadata) {
        this.actor = actor;
        this.actorEmail = actorEmail;
        this.action = action;
        this.file = file;
        this.fileNameSnapshot = fileNameSnapshot;
        this.metadata = metadata;
    }

    public Long getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public AuditAction getAction() {
        return action;
    }

    public File getFile() {
        return file;
    }

    public String getFileNameSnapshot() {
        return fileNameSnapshot;
    }

    public String getMetadata() {
        return metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
