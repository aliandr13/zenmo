package com.github.aliandr13.zenmo.auth;

/**
 * Thrown when a refresh token is missing, unknown, expired or revoked. Mapped to HTTP 401.
 */
public class InvalidRefreshTokenException extends RuntimeException {

    /**
     * Constructor.
     */
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
