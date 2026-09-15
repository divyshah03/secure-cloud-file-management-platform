package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import com.cloudfilemanager.file.File;
import com.cloudfilemanager.file.FileRepository;
import com.cloudfilemanager.file.sharing.dto.CreateShareLinkRequest;
import com.cloudfilemanager.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShareLinkServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FileShareLinkRepository shareLinkRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private ShareLinkService shareLinkService;

    private User owner;
    private File file;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shareLinkService, "frontendUrl", "http://localhost:5173");

        owner = new User("Owner", "owner@example.com", "password");
        owner.setId(1L);

        file = new File("stored.bin", "original.bin", 100L, "application/octet-stream",
                "files/1/stored.bin", "filemanager-files", owner);
        file.setId(42L);
    }

    @Test
    void resolveActiveLinkThrowsForUnknownToken() {
        when(shareLinkRepository.findByToken("bogus")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shareLinkService.resolveActiveLink("bogus"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveActiveLinkThrowsForExpiredLink() {
        FileShareLink expired = new FileShareLink(
                file, "expired-token", PermissionRole.VIEWER, owner,
                LocalDateTime.now().minusHours(1));
        when(shareLinkRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> shareLinkService.resolveActiveLink("expired-token"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void resolveActiveLinkThrowsForRevokedLink() {
        FileShareLink revoked = new FileShareLink(
                file, "revoked-token", PermissionRole.VIEWER, owner,
                LocalDateTime.now().plusHours(1));
        revoked.setRevoked(true);
        when(shareLinkRepository.findByToken("revoked-token")).thenReturn(Optional.of(revoked));

        assertThatThrownBy(() -> shareLinkService.resolveActiveLink("revoked-token"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void resolveActiveLinkSucceedsForValidUnexpiredLink() {
        FileShareLink active = new FileShareLink(
                file, "good-token", PermissionRole.EDITOR, owner,
                LocalDateTime.now().plusHours(1));
        when(shareLinkRepository.findByToken("good-token")).thenReturn(Optional.of(active));

        FileShareLink resolved = shareLinkService.resolveActiveLink("good-token");

        assertThat(resolved.getFile()).isEqualTo(file);
        assertThat(resolved.getRole()).isEqualTo(PermissionRole.EDITOR);
    }

    @Test
    void createLinkRejectsNonOwnerCaller() {
        User stranger = new User("Stranger", "stranger@example.com", "password");
        stranger.setId(99L);
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));

        CreateShareLinkRequest request = new CreateShareLinkRequest(PermissionRole.VIEWER, 24);

        assertThatThrownBy(() -> shareLinkService.createLink(42L, stranger, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createLinkSucceedsForOwnerAndSetsFutureExpiry() {
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        when(shareLinkRepository.save(any(FileShareLink.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateShareLinkRequest request = new CreateShareLinkRequest(PermissionRole.VIEWER, 24);
        var dto = shareLinkService.createLink(42L, owner, request);

        assertThat(dto.role()).isEqualTo("VIEWER");
        assertThat(dto.expiresAt()).isAfter(LocalDateTime.now());
        assertThat(dto.url()).contains(dto.token());
    }
}
