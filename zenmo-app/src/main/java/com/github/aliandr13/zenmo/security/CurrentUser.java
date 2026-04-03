package com.github.aliandr13.zenmo.security;

import java.util.UUID;

/**
 * Snapshot of the current authenticated user.
 */
public record CurrentUser(UUID userId, String email) {
}

