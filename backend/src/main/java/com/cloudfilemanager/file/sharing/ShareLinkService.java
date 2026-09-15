package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.audit.AuditAction;
import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import com.cloudfilemanager.file.File;
import com.cloudfilemanager.file.FileRepository;
import com.cloudfilemanager.file.sharing.dto.CreateShareLinkRequest;
import com.cloudfilemanager.file.sharing.dto.FileShareLinkDto;
import com.cloudfilemanager.user.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ShareLinkService {

    private final FileRepository fileRepository;
    private final FileShareLinkRepository shareLinkRepository;
    private final AuditLogService auditLogService;
    private final String frontendUrl;

    public ShareLinkService(
            FileRepository fileRepository,
            FileShareLinkRepository shareLinkRepository,
            AuditLogService auditLogService,
            @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.fileRepository = fileRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.auditLogService = auditLogService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public FileShareLinkDto createLink(Long fileId, User owner, CreateShareLinkRequest request) {
        File file = requireOwnedFile(fileId, owner);

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(request.expiresInHours());

        FileShareLink link = new FileShareLink(file, token, request.role(), owner, expiresAt);
        FileShareLink saved = shareLinkRepository.save(link);
        auditLogService.record(owner, AuditAction.SHARE_LINK_CREATE, file,
                request.role() + ", expires in " + request.expiresInHours() + "h");
        return toDto(saved);
    }

    public List<FileShareLinkDto> listLinks(Long fileId, User owner) {
        File file = requireOwnedFile(fileId, owner);
        return shareLinkRepository.findByFile(file)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void revokeLink(Long fileId, User owner, Long linkId) {
        File file = requireOwnedFile(fileId, owner);
        FileShareLink link = shareLinkRepository.findByIdAndFile(linkId, file)
                .orElseThrow(() -> new ResourceNotFoundException("Share link not found with id: " + linkId));
        link.setRevoked(true);
        shareLinkRepository.save(link);
        auditLogService.record(owner, AuditAction.SHARE_LINK_REVOKE, file, null);
    }

    public FileShareLink resolveActiveLink(String token) {
        FileShareLink link = shareLinkRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid or unknown share link"));

        if (!link.isActive()) {
            throw new ResourceNotFoundException("This share link has expired or been revoked");
        }

        return link;
    }

    private File requireOwnedFile(Long fileId, User owner) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        if (!file.getOwner().getId().equals(owner.getId())) {
            throw new ResourceNotFoundException("File not found with id: " + fileId);
        }
        return file;
    }

    private FileShareLinkDto toDto(FileShareLink link) {
        return new FileShareLinkDto(
                link.getId(),
                link.getToken(),
                frontendUrl + "/share/" + link.getToken(),
                link.getRole().name(),
                link.getExpiresAt(),
                link.isRevoked(),
                link.isExpired(),
                link.getCreatedAt()
        );
    }
}
