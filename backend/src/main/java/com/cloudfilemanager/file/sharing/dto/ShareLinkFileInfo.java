package com.cloudfilemanager.file.sharing.dto;

public record ShareLinkFileInfo(
        Long fileId,
        String originalFileName,
        Long fileSize,
        String contentType,
        String role
) {
}
