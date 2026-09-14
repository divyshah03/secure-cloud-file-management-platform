package com.cloudfilemanager.file;

import com.cloudfilemanager.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FileRepository extends JpaRepository<File, Long> {
    Page<File> findByOwner(User owner, Pageable pageable);
    List<File> findByOwner(User owner);
    Optional<File> findByIdAndOwner(Long id, User owner);
    boolean existsByIdAndOwner(Long id, User owner);
    long countByOwner(User owner);
    
    @Query("SELECT SUM(f.fileSize) FROM File f WHERE f.owner = :owner")
    Long getTotalFileSizeByOwner(@Param("owner") User owner);
}
