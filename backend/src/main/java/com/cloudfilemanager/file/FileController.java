package com.cloudfilemanager.file;

import com.cloudfilemanager.user.User;
import com.cloudfilemanager.file.dto.FileDto;
import com.cloudfilemanager.file.dto.FileUploadResponse;
import com.cloudfilemanager.file.dto.PresignedDownloadResponse;
import com.cloudfilemanager.file.dto.PresignedUploadCompleteRequest;
import com.cloudfilemanager.file.dto.PresignedUploadRequest;
import com.cloudfilemanager.file.dto.PresignedUploadResponse;
import com.cloudfilemanager.file.sharing.FilePermissionService;
import com.cloudfilemanager.file.sharing.ShareLinkService;
import com.cloudfilemanager.file.sharing.dto.CreateShareLinkRequest;
import com.cloudfilemanager.file.sharing.dto.FilePermissionDto;
import com.cloudfilemanager.file.sharing.dto.FileShareLinkDto;
import com.cloudfilemanager.file.sharing.dto.GrantPermissionRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;
    private final FilePermissionService filePermissionService;
    private final ShareLinkService shareLinkService;

    public FileController(
            FileService fileService,
            FilePermissionService filePermissionService,
            ShareLinkService shareLinkService) {
        this.fileService = fileService;
        this.filePermissionService = filePermissionService;
        this.shareLinkService = shareLinkService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileUploadResponse> uploadFile(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal User user) {
        
        FileUploadResponse response = fileService.uploadFile(file, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // ---- Presigned S3 URL flow (direct browser <-> S3, bypassing backend for bytes) ----

    @PostMapping("/presigned-upload")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PresignedUploadResponse> createPresignedUpload(
            @Valid @RequestBody PresignedUploadRequest request,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.createPresignedUpload(request, user));
    }

    @PostMapping("/presigned-upload/complete")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileUploadResponse> completePresignedUpload(
            @Valid @RequestBody PresignedUploadCompleteRequest request,
            @AuthenticationPrincipal User user) {
        FileUploadResponse response = fileService.completePresignedUpload(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{fileId}/presigned-download")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PresignedDownloadResponse> createPresignedDownload(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(fileService.createPresignedDownload(fileId, user));
    }

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Page<FileDto>> getUserFiles(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir,
            @AuthenticationPrincipal User user) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<FileDto> files = fileService.getUserFiles(user, pageable);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<FileDto>> getAllUserFiles(@AuthenticationPrincipal User user) {
        List<FileDto> files = fileService.getAllUserFiles(user);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/shared-with-me")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<FileDto>> getFilesSharedWithMe(@AuthenticationPrincipal User user) {
        List<FileDto> files = fileService.getFilesSharedWithMe(user);
        return ResponseEntity.ok(files);
    }

    @GetMapping("/{fileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileDto> getFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {
        FileDto file = fileService.getFileById(fileId, user);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/{fileId}/download")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {
        
        FileDto fileDto = fileService.getFileById(fileId, user);
        byte[] fileData = fileService.downloadFile(fileId, user);

        ByteArrayResource resource = new ByteArrayResource(fileData);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileDto.originalFileName() + "\"")
                .contentType(MediaType.parseMediaType(fileDto.contentType()))
                .contentLength(fileData.length)
                .body(resource);
    }

    @DeleteMapping("/{fileId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> deleteFile(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User user) {
        fileService.deleteFile(fileId, user);
        return ResponseEntity.ok(Map.of("message", "File deleted successfully"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, Object>> getFileStats(@AuthenticationPrincipal User user) {
        long fileCount = fileService.getUserFileCount(user);
        long totalSize = fileService.getUserTotalFileSize(user);

        return ResponseEntity.ok(Map.of(
                "fileCount", fileCount,
                "totalSize", totalSize,
                "totalSizeMB", String.format("%.2f", totalSize / (1024.0 * 1024.0))
        ));
    }

    // ---- Collaborator permissions (owner-only) ----

    @GetMapping("/{fileId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<FilePermissionDto>> listPermissions(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(filePermissionService.listPermissions(fileId, owner));
    }

    @PostMapping("/{fileId}/permissions")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FilePermissionDto> grantPermission(
            @PathVariable Long fileId,
            @Valid @RequestBody GrantPermissionRequest request,
            @AuthenticationPrincipal User owner) {
        FilePermissionDto dto = filePermissionService.grantPermission(fileId, owner, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{fileId}/permissions/{userId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> revokePermission(
            @PathVariable Long fileId,
            @PathVariable Long userId,
            @AuthenticationPrincipal User owner) {
        filePermissionService.revokePermission(fileId, owner, userId);
        return ResponseEntity.ok(Map.of("message", "Permission revoked successfully"));
    }

    // ---- Shareable links (owner-only to manage; redemption is public, see ShareController) ----

    @GetMapping("/{fileId}/share-links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<FileShareLinkDto>> listShareLinks(
            @PathVariable Long fileId,
            @AuthenticationPrincipal User owner) {
        return ResponseEntity.ok(shareLinkService.listLinks(fileId, owner));
    }

    @PostMapping("/{fileId}/share-links")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<FileShareLinkDto> createShareLink(
            @PathVariable Long fileId,
            @Valid @RequestBody CreateShareLinkRequest request,
            @AuthenticationPrincipal User owner) {
        FileShareLinkDto dto = shareLinkService.createLink(fileId, owner, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{fileId}/share-links/{linkId}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<String, String>> revokeShareLink(
            @PathVariable Long fileId,
            @PathVariable Long linkId,
            @AuthenticationPrincipal User owner) {
        shareLinkService.revokeLink(fileId, owner, linkId);
        return ResponseEntity.ok(Map.of("message", "Share link revoked successfully"));
    }
}
