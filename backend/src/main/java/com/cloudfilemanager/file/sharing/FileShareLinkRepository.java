package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.file.File;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FileShareLinkRepository extends JpaRepository<FileShareLink, Long> {
    Optional<FileShareLink> findByToken(String token);
    List<FileShareLink> findByFile(File file);
    Optional<FileShareLink> findByIdAndFile(Long id, File file);
}
