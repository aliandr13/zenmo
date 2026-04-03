package com.github.aliandr13.zenmo.auth.dto;

/**
 * Response DTO with access and refresh tokens.
 */
public record TokensResponse(String accessToken, String refreshToken) {
}

