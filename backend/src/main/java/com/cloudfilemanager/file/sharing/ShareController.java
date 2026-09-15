package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.audit.AuditAction;
import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.file.File;
import com.cloudfilemanager.file.dto.PresignedDownloadResponse;
import com.cloudfilemanager.file.sharing.dto.ShareLinkFileInfo;
import com.cloudfilemanager.storage.S3Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/share")
public class ShareController {

    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15);

    private final ShareLinkService shareLinkService;
    private final S3Service s3Service;
    private final AuditLogService auditLogService;
    private final boolean mockStorage;

    public ShareController(
            ShareLinkService shareLinkService,
            S3Service s3Service,
            AuditLogService auditLogService,
            @Value("${aws.s3.mock:true}") boolean mockStorage) {
        this.shareLinkService = shareLinkService;
        this.s3Service = s3Service;
        this.auditLogService = auditLogService;
        this.mockStorage = mockStorage;
    }

    @GetMapping("/{token}")
    public ResponseEntity<ShareLinkFileInfo> getSharedFileInfo(@PathVariable String token) {
        FileShareLink link = shareLinkService.resolveActiveLink(token);
        File file = link.getFile();
        return ResponseEntity.ok(new ShareLinkFileInfo(
                file.getId(),
                file.getOriginalFileName(),
                file.getFileSize(),
                file.getContentType(),
                link.getRole().name()
        ));
    }

    @GetMapping("/{token}/presigned-download")
    public ResponseEntity<PresignedDownloadResponse> getPresignedSharedDownload(@PathVariable String token) {
        if (mockStorage) {
            throw new IllegalStateException(
                    "Presigned URLs are not available when running against mock S3 storage");
        }
        FileShareLink link = shareLinkService.resolveActiveLink(token);
        File file = link.getFile();
        auditLogService.record(null, AuditAction.SHARE_LINK_REDEEM, file, "via presigned URL, token " + token);

        String disposition = "attachment; filename=\"" + file.getOriginalFileName() + "\"";
        URI downloadUrl = s3Service.presignGetObject(
                file.getS3Bucket(), file.getS3Key(), disposition, PRESIGNED_URL_EXPIRY);

        return ResponseEntity.ok(new PresignedDownloadResponse(
                downloadUrl.toString(),
                file.getOriginalFileName(),
                LocalDateTime.now().plus(PRESIGNED_URL_EXPIRY)
        ));
    }

    @GetMapping("/{token}/download")
    public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
        FileShareLink link = shareLinkService.resolveActiveLink(token);
        File file = link.getFile();
        auditLogService.record(null, AuditAction.SHARE_LINK_REDEEM, file, "token " + token);

        byte[] fileData = s3Service.getObject(file.getS3Bucket(), file.getS3Key());
        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + file.getOriginalFileName() + "\"")
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(fileData.length)
                .body(resource);
    }
}
