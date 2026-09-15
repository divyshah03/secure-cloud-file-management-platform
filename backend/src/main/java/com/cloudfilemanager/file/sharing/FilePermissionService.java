package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.audit.AuditAction;
import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.common.exception.DuplicateResourceException;
import com.cloudfilemanager.common.exception.ForbiddenOperationException;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import com.cloudfilemanager.file.File;
import com.cloudfilemanager.file.FileRepository;
import com.cloudfilemanager.file.sharing.dto.FilePermissionDto;
import com.cloudfilemanager.file.sharing.dto.GrantPermissionRequest;
import com.cloudfilemanager.user.User;
import com.cloudfilemanager.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FilePermissionService {

    private final FileRepository fileRepository;
    private final FilePermissionRepository filePermissionRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public FilePermissionService(
            FileRepository fileRepository,
            FilePermissionRepository filePermissionRepository,
            UserRepository userRepository,
            AuditLogService auditLogService) {
        this.fileRepository = fileRepository;
        this.filePermissionRepository = filePermissionRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    public record SharedFile(File file, EffectiveRole role) {
    }

    public List<SharedFile> findFilesSharedWith(User user) {
        return filePermissionRepository.findByUser(user)
                .stream()
                .map(p -> new SharedFile(
                        p.getFile(),
                        p.getRole() == PermissionRole.EDITOR ? EffectiveRole.EDITOR : EffectiveRole.VIEWER))
                .collect(Collectors.toList());
    }

    public EffectiveRole effectiveRole(File file, User user) {
        if (file.getOwner().getId().equals(user.getId())) {
            return EffectiveRole.OWNER;
        }
        return filePermissionRepository.findByFileAndUser(file, user)
                .map(p -> p.getRole() == PermissionRole.EDITOR ? EffectiveRole.EDITOR : EffectiveRole.VIEWER)
                .orElse(EffectiveRole.NONE);
    }

    public void requireAtLeast(File file, User user, EffectiveRole minimum) {
        EffectiveRole role = effectiveRole(file, user);
        if (role == EffectiveRole.NONE) {
            throw new ResourceNotFoundException("File not found with id: " + file.getId());
        }
        if (!role.atLeast(minimum)) {
            throw new ForbiddenOperationException(
                    "You do not have permission to perform this action on this file");
        }
    }

    @Transactional
    public FilePermissionDto grantPermission(Long fileId, User owner, GrantPermissionRequest request) {
        File file = requireOwnedFile(fileId, owner);

        User target = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No user found with email: " + request.email()));

        if (target.getId().equals(owner.getId())) {
            throw new DuplicateResourceException("Cannot grant a permission to the file owner");
        }

        FilePermission permission = filePermissionRepository.findByFileAndUser(file, target)
                .orElseGet(() -> new FilePermission(file, target, request.role(), owner));
        permission.setRole(request.role());

        FilePermission saved = filePermissionRepository.save(permission);
        auditLogService.record(owner, AuditAction.PERMISSION_GRANT, file,
                target.getEmail() + " granted " + request.role());
        return toDto(saved);
    }

    @Transactional
    public void revokePermission(Long fileId, User owner, Long targetUserId) {
        File file = requireOwnedFile(fileId, owner);
        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + targetUserId));
        filePermissionRepository.deleteByFileAndUser(file, target);
        auditLogService.record(owner, AuditAction.PERMISSION_REVOKE, file, target.getEmail() + " revoked");
    }

    public List<FilePermissionDto> listPermissions(Long fileId, User owner) {
        File file = requireOwnedFile(fileId, owner);
        return filePermissionRepository.findByFile(file)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private File requireOwnedFile(Long fileId, User owner) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("File not found with id: " + fileId);
        }
        return file;
    }

    private FilePermissionDto toDto(FilePermission permission) {
        return new FilePermissionDto(
                permission.getId(),
                permission.getUser().getId(),
                permission.getUser().getName(),
                permission.getUser().getEmail(),
                permission.getRole().name(),
                permission.getCreatedAt()
        );
    }
}
