package com.cloudfilemanager.file;

import com.cloudfilemanager.file.dto.FileDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class FileDtoMapper implements Function<File, FileDto> {

    private final String baseUrl;

    public FileDtoMapper(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl + "/api/v1/files";
    }

    @Override
    public FileDto apply(File file) {
        return toDto(file, "OWNER");
    }

    public FileDto toDto(File file, String role) {
        return new FileDto(
                file.getId(),
                file.getFileName(),
                file.getOriginalFileName(),
                file.getFileSize(),
                file.getContentType(),
                baseUrl + "/" + file.getId() + "/download",
                file.getCreatedAt(),
                file.getOwner().getId(),
                role
        );
    }
}
