package com.github.aliandr13.zenmo.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JPA repository for refresh tokens.
 */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    /**
     * Finds a token by its hash.
     */
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /**
     * Deletes expired tokens.
     */
    long deleteByExpiresAtBefore(Instant instant);
}

