package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.file.File;
import com.cloudfilemanager.user.User;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_permissions", uniqueConstraints = {
        @UniqueConstraint(name = "uq_file_permissions_file_user", columnNames = {"file_id", "user_id"})
})
public class FilePermission {

    @Id
    @SequenceGenerator(name = "file_permission_id_seq", sequenceName = "file_permission_id_seq", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_permission_id_seq")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private File file;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PermissionRole role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "granted_by")
    private User grantedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public FilePermission() {
    }

    public FilePermission(File file, User user, PermissionRole role, User grantedBy) {
        this.file = file;
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
    }

    public Long getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public User getUser() {
        return user;
    }

    public PermissionRole getRole() {
        return role;
    }

    public void setRole(PermissionRole role) {
        this.role = role;
    }

    public User getGrantedBy() {
        return grantedBy;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
