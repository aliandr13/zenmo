package com.github.aliandr13.zenmo.auth.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for token refresh.
 */
public record RefreshRequest(@NotBlank String refreshToken) {
}

