package com.cloudfilemanager.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record EmailVerificationRequest(
        @NotBlank(message = "Verification token is required")
        String token
) {
}
