package com.github.aliandr13.zenmo.auth;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Refresh token domain object (plain POJO).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    private UUID id;
    private UUID userId;
    private String tokenHash;
    private Instant expiresAt;
    private Instant revokedAt;
    private Instant createdAt;

    /**
     * Whether this token has been revoked.
     */
    public boolean isRevoked() {
        return revokedAt != null;
    }
}
