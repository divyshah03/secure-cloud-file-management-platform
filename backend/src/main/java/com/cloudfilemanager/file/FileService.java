package com.cloudfilemanager.file;

import com.cloudfilemanager.user.User;
import com.cloudfilemanager.storage.S3Buckets;
import com.cloudfilemanager.storage.S3Service;
import com.cloudfilemanager.file.dto.FileDTO;
import com.cloudfilemanager.file.dto.FileUploadResponse;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileService {

    private static final long MAX_FILE_SIZE = 50 * 1024 * 1024; // 50 MB

    private final FileRepository fileRepository;
    private final FileDTOMapper fileDTOMapper;
    private final S3Service s3Service;
    private final S3Buckets s3Buckets;

    public FileService(
            FileRepository fileRepository,
            FileDTOMapper fileDTOMapper,
            S3Service s3Service,
            S3Buckets s3Buckets) {
        this.fileRepository = fileRepository;
        this.fileDTOMapper = fileDTOMapper;
        this.s3Service = s3Service;
        this.s3Buckets = s3Buckets;
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

    public FileDTO getFileById(Long fileId, User owner) {
        File file = fileRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found with id: " + fileId));
        return fileDTOMapper.apply(file);
    }

    public Page<FileDTO> getUserFiles(User owner, Pageable pageable) {
        return fileRepository.findByOwner(owner, pageable)
                .map(fileDTOMapper);
    }

    public List<FileDTO> getAllUserFiles(User owner) {
        return fileRepository.findByOwner(owner)
                .stream()
                .map(fileDTOMapper)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFile(Long fileId, User owner) {
        File file = fileRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found with id: " + fileId));

        try {
            s3Service.deleteObject(file.getS3Bucket(), file.getS3Key());
            fileRepository.delete(file);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public byte[] downloadFile(Long fileId, User owner) {
        File file = fileRepository.findByIdAndOwner(fileId, owner)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "File not found with id: " + fileId));

        return s3Service.getObject(file.getS3Bucket(), file.getS3Key());
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
