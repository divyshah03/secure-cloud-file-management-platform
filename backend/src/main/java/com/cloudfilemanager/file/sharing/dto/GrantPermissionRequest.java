package com.cloudfilemanager.file.sharing.dto;

import com.cloudfilemanager.file.sharing.PermissionRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;

public record GrantPermissionRequest(
        @NotNull @Email String email,
        @NotNull PermissionRole role
) {
}
