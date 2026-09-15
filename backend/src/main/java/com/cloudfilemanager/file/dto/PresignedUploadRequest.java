package com.cloudfilemanager.file.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PresignedUploadRequest(
        @NotBlank String originalFileName,
        @NotBlank String contentType,
        @NotNull @Min(1) @Max(50L * 1024 * 1024) Long fileSize
) {
}
