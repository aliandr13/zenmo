package com.github.aliandr13.zenmo.auth;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository for refresh tokens.
 */
public interface RefreshTokenRepository {

    /**
     * Finds a token by its hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Deletes expired tokens.
     */
    long deleteByExpiresAtBefore(Instant instant);

    /**
     * Saves a refresh token.
     */
    void save(RefreshToken token);

    /**
     * Deletes a refresh token.
     */
    void delete(RefreshToken token);
}
