package com.github.aliandr13.zenmo.user;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for users.
 */
public interface AppUserRepository {

    /**
     * Finds a user by email (case-insensitive).
     */
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /**
     * Returns whether a user exists with the given email (case-insensitive).
     */
    boolean existsByEmailIgnoreCase(String email);

    /**
     * Finds a user by id.
     */
    Optional<AppUser> findById(UUID id);

    /**
     * Saves a user.
     */
    void save(AppUser user);
}
