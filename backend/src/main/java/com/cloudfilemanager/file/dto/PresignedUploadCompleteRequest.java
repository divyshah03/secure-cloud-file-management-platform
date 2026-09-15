package com.cloudfilemanager.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUploadCompleteRequest(
        @NotBlank String s3Key,
        @NotBlank String originalFileName,
        @NotBlank String contentType,
        @NotNull Long fileSize
) {
}
