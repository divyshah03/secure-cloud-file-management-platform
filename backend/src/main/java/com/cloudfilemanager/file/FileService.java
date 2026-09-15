package com.cloudfilemanager.file;

import com.cloudfilemanager.audit.AuditAction;
import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.user.User;
import com.cloudfilemanager.storage.S3Buckets;
import com.cloudfilemanager.storage.S3Service;
import com.cloudfilemanager.file.dto.FileDto;
import com.cloudfilemanager.file.dto.FileUploadResponse;
import com.cloudfilemanager.file.dto.PresignedDownloadResponse;
import com.cloudfilemanager.file.dto.PresignedUploadCompleteRequest;
import com.cloudfilemanager.file.dto.PresignedUploadRequest;
import com.cloudfilemanager.file.dto.PresignedUploadResponse;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import com.cloudfilemanager.file.sharing.EffectiveRole;
import com.cloudfilemanager.file.sharing.FilePermissionService;
import com.cloudfilemanager.malware.MalwareDetectedException;
import com.cloudfilemanager.malware.MalwareScanner;
import com.cloudfilemanager.malware.ScanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB
    private static final Duration PRESIGNED_URL_EXPIRY = Duration.ofMinutes(15);

    private final FileRepository fileRepository;
    private final FileDtoMapper fileDtoMapper;
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;
    private final FilePermissionService filePermissionService;
    private final MalwareScanner malwareScanner;
    private final AuditLogService auditLogService;
    private final boolean mockStorage;

    public FileService(
            FileRepository fileRepository,
            FileDtoMapper fileDtoMapper,
            S3Service s3Service,
            S3Buckets s3Buckets,
            FilePermissionService filePermissionService,
            MalwareScanner malwareScanner,
            AuditLogService auditLogService,
            @Value("${aws.s3.mock:true}") boolean mockStorage) {
        this.fileRepository = fileRepository;
        this.fileDtoMapper = fileDtoMapper;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
        this.filePermissionService = filePermissionService;
        this.malwareScanner = malwareScanner;
        this.auditLogService = auditLogService;
        this.mockStorage = mockStorage;
    }

    @Transactional
    public FileUploadResponse uploadFile(MultipartFile multipartFile, User owner) {
        if (multipartFile.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (multipartFile.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum allowed size of 50 MB");
        }

        String originalFileName = multipartFile.getOriginalFilename();
        if (originalFileName == null || originalFileName.trim().isEmpty()) {
            throw new IllegalArgumentException("File name cannot be empty");
        }

        String fileExtension = getFileExtension(originalFileName);
        String fileName = UUID.randomUUID().toString() + fileExtension;
        String s3Key = "files/" + owner.getId() + "/" + fileName;

        try {
            byte[] fileBytes = multipartFile.getBytes();

            ScanResult scanResult = malwareScanner.scan(fileBytes);
            if (scanResult.infected()) {
                throw new MalwareDetectedException(scanResult.signature());
            }

            s3Service.putObject(s3Buckets.getFiles(), s3Key, fileBytes);

            File file = new File(
                    fileName,
                    originalFileName,
                    multipartFile.getSize(),
                    multipartFile.getContentType(),
                    s3Key,
                    s3Buckets.getFiles(),
                    owner
            );

            File savedFile = fileRepository.save(file);
            auditLogService.record(owner, AuditAction.UPLOAD, savedFile, null);

            return new FileUploadResponse(
                    savedFile.getId(),
                    savedFile.getFileName(),
                    savedFile.getOriginalFileName(),
                    savedFile.getFileSize(),
                    savedFile.getContentType(),
                    "File uploaded successfully"
            );
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload file", e);
        }
    }

    public FileDto getFileById(Long fileId, User user) {
        Optional<File> ownedFile = fileRepository.findByIdAndOwner(fileId, user);
        if (ownedFile.isPresent()) {
            return fileDtoMapper.apply(ownedFile.get());
        }

        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        EffectiveRole role = filePermissionService.effectiveRole(file, user);
        if (role == EffectiveRole.NONE) {
            throw new ResourceNotFoundException("File not found with id: " + fileId);
        }
        return fileDtoMapper.toDto(file, role.name());
    }

    public Page<FileDto> getUserFiles(User owner, Pageable pageable) {
        return fileRepository.findByOwner(owner, pageable)
                .map(fileDtoMapper);
    }

    public List<FileDto> getAllUserFiles(User owner) {
        return fileRepository.findByOwner(owner)
                .stream()
                .map(fileDtoMapper)
                .collect(Collectors.toList());
    }

    public List<FileDto> getFilesSharedWithMe(User user) {
        return filePermissionService.findFilesSharedWith(user)
                .stream()
                .map(entry -> fileDtoMapper.toDto(entry.file(), entry.role().name()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long fileId, User user) {
        File file = requireAccessibleFile(fileId, user, EffectiveRole.EDITOR);

        try {
            s3Service.deleteObject(file.getS3Bucket(), file.getS3Key());
            auditLogService.record(user, AuditAction.DELETE, file, null);
            fileRepository.delete(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public byte[] downloadFile(Long fileId, User user) {
        File file = requireAccessibleFile(fileId, user, EffectiveRole.VIEWER);
        auditLogService.record(user, AuditAction.DOWNLOAD, file, null);
        return s3Service.getObject(file.getS3Bucket(), file.getS3Key());
    }

    private File requireAccessibleFile(Long fileId, User user, EffectiveRole minimum) {
        Optional<File> ownedFile = fileRepository.findByIdAndOwner(fileId, user);
        if (ownedFile.isPresent()) {
            return ownedFile.get();
        }
        File found = fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File not found with id: " + fileId));
        filePermissionService.requireAtLeast(found, user, minimum);
        return found;
    }

    private void requireRealStorage() {
        if (mockStorage) {
            throw new IllegalStateException(
                    "Presigned URLs are not available when running against mock S3 storage " +
                            "(AWS_S3_MOCK=true). Use the standard upload/download endpoints instead, " +
                            "or run via docker-compose with MinIO enabled.");
        }
    }

    public PresignedUploadResponse createPresignedUpload(PresignedUploadRequest request, User owner) {
        requireRealStorage();

        String fileExtension = getFileExtension(request.originalFileName());
        String fileName = UUID.randomUUID().toString() + fileExtension;
        String s3Key = "files/" + owner.getId() + "/" + fileName;

        URI uploadUrl = s3Service.presignPutObject(
                s3Buckets.getFiles(), s3Key, request.contentType(), PRESIGNED_URL_EXPIRY);

        return new PresignedUploadResponse(
                uploadUrl.toString(),
                s3Key,
                LocalDateTime.now().plus(PRESIGNED_URL_EXPIRY)
        );
    }

    @Transactional
    public FileUploadResponse completePresignedUpload(PresignedUploadCompleteRequest request, User owner) {
        requireRealStorage();

        String expectedPrefix = "files/" + owner.getId() + "/";
        if (!request.s3Key().startsWith(expectedPrefix)) {
            throw new IllegalArgumentException("Invalid upload key for this user");
        }

        Long actualSize = s3Service.headObject(s3Buckets.getFiles(), request.s3Key())
                .orElseThrow(() -> new IllegalStateException(
                        "Upload not found in storage - the presigned upload may not have completed. " +
                                "Please retry the upload."));

        byte[] uploadedBytes = s3Service.getObject(s3Buckets.getFiles(), request.s3Key());
        ScanResult scanResult = malwareScanner.scan(uploadedBytes);
        if (scanResult.infected()) {
            s3Service.deleteObject(s3Buckets.getFiles(), request.s3Key());
            throw new MalwareDetectedException(scanResult.signature());
        }

        File file = new File(
                request.s3Key().substring(expectedPrefix.length()),
                request.originalFileName(),
                actualSize,
                request.contentType(),
                request.s3Key(),
                s3Buckets.getFiles(),
                owner
        );

        File savedFile = fileRepository.save(file);
        auditLogService.record(owner, AuditAction.UPLOAD, savedFile, "via presigned URL");

        return new FileUploadResponse(
                savedFile.getId(),
                savedFile.getFileName(),
                savedFile.getOriginalFileName(),
                savedFile.getFileSize(),
                savedFile.getContentType(),
                "File uploaded successfully"
        );
    }

    public PresignedDownloadResponse createPresignedDownload(Long fileId, User user) {
        requireRealStorage();
        File file = requireAccessibleFile(fileId, user, EffectiveRole.VIEWER);
        auditLogService.record(user, AuditAction.DOWNLOAD, file, "via presigned URL");

        String disposition = "attachment; filename=\"" + file.getOriginalFileName() + "\"";
        URI downloadUrl = s3Service.presignGetObject(
                file.getS3Bucket(), file.getS3Key(), disposition, PRESIGNED_URL_EXPIRY);

        return new PresignedDownloadResponse(
                downloadUrl.toString(),
                file.getOriginalFileName(),
                LocalDateTime.now().plus(PRESIGNED_URL_EXPIRY)
        );
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(lastDotIndex);
    }

    public long getUserFileCount(User owner) {
        return fileRepository.countByOwner(owner);
    }

    public long getUserTotalFileSize(User owner) {
        Long totalSize = fileRepository.getTotalFileSizeByOwner(owner);
        return totalSize != null ? totalSize : 0L;
    }
}
