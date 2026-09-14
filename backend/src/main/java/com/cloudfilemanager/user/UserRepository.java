package com.cloudfilemanager.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByEmailVerificationToken(String token);
    
    @Modifying
    @Query("UPDATE User u SET u.enabled = true, u.emailVerifiedAt = :verifiedAt, u.emailVerificationToken = null, u.emailVerificationTokenExpiresAt = null WHERE u.emailVerificationToken = :token")
    int verifyEmail(@Param("token") String token, @Param("verifiedAt") LocalDateTime verifiedAt);
}
