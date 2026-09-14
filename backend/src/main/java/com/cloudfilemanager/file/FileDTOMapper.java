package com.cloudfilemanager.file;

import com.cloudfilemanager.file.dto.FileDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class FileDTOMapper implements Function<File, FileDTO> {
    
    private final String baseUrl;
    
    public FileDTOMapper(@Value("${app.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl + "/api/v1/files";
    }
    
    @Override
    public FileDTO apply(File file) {
        return new FileDTO(
                file.getId(),
                file.getFileName(),
                file.getOriginalFileName(),
                file.getFileSize(),
                file.getContentType(),
                baseUrl + "/" + file.getId() + "/download",
                file.getCreatedAt(),
                file.getOwner().getId()
        );
    }
}
