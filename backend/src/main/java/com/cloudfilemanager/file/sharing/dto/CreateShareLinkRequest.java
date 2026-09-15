package com.cloudfilemanager.file.sharing.dto;

import com.cloudfilemanager.file.sharing.PermissionRole;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateShareLinkRequest(
        @NotNull PermissionRole role,
        @Min(1) @Max(8760) int expiresInHours
) {
}
