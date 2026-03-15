package com.github.aliandr13.zenmo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for login.
 */
public record LoginRequest(
        @Email @NotBlank String email,
        @NotBlank String password
) {
}

