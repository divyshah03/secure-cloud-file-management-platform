package com.cloudfilemanager.file.sharing;

import com.cloudfilemanager.audit.AuditLogService;
import com.cloudfilemanager.common.exception.ForbiddenOperationException;
import com.cloudfilemanager.common.exception.ResourceNotFoundException;
import com.cloudfilemanager.file.File;
import com.cloudfilemanager.file.FileRepository;
import com.cloudfilemanager.file.sharing.dto.GrantPermissionRequest;
import com.cloudfilemanager.user.User;
import com.cloudfilemanager.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilePermissionServiceTest {

    @Mock
    private FileRepository fileRepository;

    @Mock
    private FilePermissionRepository filePermissionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FilePermissionService filePermissionService;

    private User owner;
    private User editor;
    private User viewer;
    private User stranger;
    private File file;

    @BeforeEach
    void setUp() {
        owner = user(1L, "owner@example.com");
        editor = user(2L, "editor@example.com");
        viewer = user(3L, "viewer@example.com");
        stranger = user(4L, "stranger@example.com");

        file = new File("stored.bin", "original.bin", 100L, "application/octet-stream",
                "files/1/stored.bin", "filemanager-files", owner);
        file.setId(42L);
    }

    private User user(Long id, String email) {
        User u = new User("Name", email, "password");
        u.setId(id);
        return u;
    }

    // ---- Role-permission matrix: who has what effective role ----

    @Test
    void ownerHasOwnerEffectiveRole() {
        assertThat(filePermissionService.effectiveRole(file, owner)).isEqualTo(EffectiveRole.OWNER);
    }

    @Test
    void userWithEditorGrantHasEditorEffectiveRole() {
        FilePermission grant = new FilePermission(file, editor, PermissionRole.EDITOR, owner);
        when(filePermissionRepository.findByFileAndUser(file, editor)).thenReturn(Optional.of(grant));

        assertThat(filePermissionService.effectiveRole(file, editor)).isEqualTo(EffectiveRole.EDITOR);
    }

    @Test
    void userWithViewerGrantHasViewerEffectiveRole() {
        FilePermission grant = new FilePermission(file, viewer, PermissionRole.VIEWER, owner);
        when(filePermissionRepository.findByFileAndUser(file, viewer)).thenReturn(Optional.of(grant));

        assertThat(filePermissionService.effectiveRole(file, viewer)).isEqualTo(EffectiveRole.VIEWER);
    }

    @Test
    void userWithNoGrantHasNoneEffectiveRole() {
        when(filePermissionRepository.findByFileAndUser(file, stranger)).thenReturn(Optional.empty());

        assertThat(filePermissionService.effectiveRole(file, stranger)).isEqualTo(EffectiveRole.NONE);
    }

    // ---- requireAtLeast: who can do what ----

    @ParameterizedTest
    @CsvSource({
            "OWNER, VIEWER, true",
            "OWNER, EDITOR, true",
            "OWNER, OWNER, true",
            "EDITOR, VIEWER, true",
            "EDITOR, EDITOR, true",
            "EDITOR, OWNER, false",
            "VIEWER, VIEWER, true",
            "VIEWER, EDITOR, false",
            "VIEWER, OWNER, false",
            "NONE, VIEWER, false",
            "NONE, EDITOR, false",
            "NONE, OWNER, false"
    })
    void requireAtLeastMatrix(EffectiveRole actual, EffectiveRole required, boolean shouldSucceed) {
        assertThat(actual.atLeast(required)).isEqualTo(shouldSucceed);
    }

    @Test
    void requireAtLeastThrowsNotFoundWhenNoRelationToFile() {
        when(filePermissionRepository.findByFileAndUser(file, stranger)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> filePermissionService.requireAtLeast(file, stranger, EffectiveRole.VIEWER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void requireAtLeastThrowsForbiddenWhenViewerAttemptsEditorAction() {
        FilePermission grant = new FilePermission(file, viewer, PermissionRole.VIEWER, owner);
        when(filePermissionRepository.findByFileAndUser(file, viewer)).thenReturn(Optional.of(grant));

        assertThatThrownBy(() -> filePermissionService.requireAtLeast(file, viewer, EffectiveRole.EDITOR))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void requireAtLeastSucceedsForOwnerRegardlessOfMinimum() {
        filePermissionService.requireAtLeast(file, owner, EffectiveRole.EDITOR);
        // no exception thrown = success
    }

    // ---- Granting/revoking ----

    @Test
    void grantPermissionRejectsGrantingToOwnerThemselves() {
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(owner));

        GrantPermissionRequest request = new GrantPermissionRequest("owner@example.com", PermissionRole.EDITOR);

        assertThatThrownBy(() -> filePermissionService.grantPermission(42L, owner, request))
                .isInstanceOf(com.cloudfilemanager.common.exception.DuplicateResourceException.class);
    }

    @Test
    void grantPermissionRejectsNonOwnerCaller() {
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));

        GrantPermissionRequest request = new GrantPermissionRequest("viewer@example.com", PermissionRole.VIEWER);

        assertThatThrownBy(() -> filePermissionService.grantPermission(42L, stranger, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void grantPermissionSucceedsForOwner() {
        when(fileRepository.findById(42L)).thenReturn(Optional.of(file));
        when(userRepository.findByEmail("viewer@example.com")).thenReturn(Optional.of(viewer));
        when(filePermissionRepository.findByFileAndUser(file, viewer)).thenReturn(Optional.empty());
        when(filePermissionRepository.save(any(FilePermission.class))).thenAnswer(inv -> inv.getArgument(0));

        GrantPermissionRequest request = new GrantPermissionRequest("viewer@example.com", PermissionRole.VIEWER);
        var dto = filePermissionService.grantPermission(42L, owner, request);

        assertThat(dto.userEmail()).isEqualTo("viewer@example.com");
        assertThat(dto.role()).isEqualTo("VIEWER");
    }
}
