package com.cloudfilemanager.file.dto;

public record FileUploadResponse(
        Long fileId,
        String fileName,
        String originalFileName,
        Long fileSize,
        String contentType,
        String message
) {
}
