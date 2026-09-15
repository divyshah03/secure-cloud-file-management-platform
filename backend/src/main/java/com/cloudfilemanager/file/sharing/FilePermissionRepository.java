package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.file.File;
import com.cloudfilemanager.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FilePermissionRepository extends JpaRepository<FilePermission, Long> {
    Optional<FilePermission> findByFileAndUser(File file, User user);
    List<FilePermission> findByFile(File file);
    List<FilePermission> findByUser(User user);
    void deleteByFileAndUser(File file, User user);
}
