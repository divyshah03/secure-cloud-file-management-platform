package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.file.File;
import com.cloudfilemanager.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_share_links")
public class FileShareLink {

    @Id
    @SequenceGenerator(name = "file_share_link_id_seq", sequenceName = "file_share_link_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_share_link_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public FileShareLink() {
    }

    public FileShareLink(File file, String token, PermissionRole role, User createdBy, LocalDateTime expiresAt) {
        this.file = file;
        this.token = token;
        this.role = role;
        this.createdBy = createdBy;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public Long getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public String getToken() {
        return token;
    }

    public PermissionRole getRole() {
        return role;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
